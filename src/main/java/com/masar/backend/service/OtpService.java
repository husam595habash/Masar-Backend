package com.masar.backend.service;

import com.masar.backend.exception.EmailSendException;
import com.masar.backend.exception.InvalidOtpException;
import com.masar.backend.exception.TooManyRequestsException;
import com.masar.backend.infra.EmailService;
import com.masar.backend.infra.OtpStore;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Everything about verification codes: create and email a code, check a code,
 * and the rules around it (expiry, resend cooldown, max wrong attempts).
 * It knows nothing about users or tokens: AuthService decides what a valid code means.
 */
@Service
public class OtpService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OtpService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    /** How long a code is valid. */
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    /** Minimum time between two emails for the same user. */
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    /** Wrong guesses allowed before the code is locked. */
    private static final int MAX_ATTEMPTS = 5;

    private final OtpStore otpStore;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public OtpService(OtpStore otpStore,
                      EmailService emailService,
                      PasswordEncoder passwordEncoder) {
        this.otpStore = otpStore;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new code for this email, stores only its hash in Redis (replacing any old
     * code, expires after 5 minutes), then emails the plain code.
     * Throws EmailSendException if sending fails.
     */
    public void issueOtp(String email) {
        requireCooldownPassed(email);

        String otp = generateOtp();

        otpStore.save(email, passwordEncoder.encode(otp), OTP_TTL);
        try {
            emailService.sendOtp(email, otp);
        } catch (MailException e) {
            log.error("Could not send OTP email to {}", email, e);   // shows the real cause in the console
            otpStore.delete(email);   // nothing was delivered, so do not make the user wait
            throw new EmailSendException();
        }
    }

    /**
     * Checks the code for this email. Returns normally if it is correct, otherwise throws:
     * InvalidOtpException (expired, missing or wrong) or TooManyRequestsException (locked).
     * Wrong guesses are counted in Redis (max 5). The code is NOT deleted here, call
     * {@link #clear(String)} once the caller has finished its own work.
     */
    public void verify(String email, String otp) {
        // Redis deletes the code after 5 minutes, so "not found" also covers "expired"
        String codeHash = otpStore.getHash(email)
                .orElseThrow(() -> new InvalidOtpException("Code expired or not found, please request a new one"));

        if (otpStore.getAttempts(email) >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Too many wrong attempts, please request a new code");
        }

        if (!passwordEncoder.matches(otp, codeHash)) {
            otpStore.incrementAttempts(email);
            throw new InvalidOtpException("Invalid code");
        }
    }

    /** Deletes the code and the cooldown of this email. */
    public void clear(String email) {
        otpStore.delete(email);
    }

    /**
     * Starts the resend cooldown. Throws TooManyRequestsException (429) if the user
     * must still wait.
     */
    private void requireCooldownPassed(String email) {
        if (!otpStore.tryStartCooldown(email, RESEND_COOLDOWN)) {
            throw new TooManyRequestsException(
                    "Please wait " + otpStore.cooldownSecondsLeft(email) + " seconds before requesting a new code");
        }
    }

    /** Generates a 6-digit OTP as a String (leading zeros kept). */
    private String generateOtp() {
        int number = RANDOM.nextInt(1_000_000);  // 0 to 999999
        return String.format("%06d", number);  // 7 -> "000007"
    }
}
