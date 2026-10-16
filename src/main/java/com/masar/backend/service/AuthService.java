package com.masar.backend.service;

import com.masar.backend.security.JwtService;
import com.masar.backend.infra.EmailService;
import com.masar.backend.infra.OtpStore;
import com.masar.backend.dto.auth.AuthResponse;
import com.masar.backend.dto.auth.LoginRequest;
import com.masar.backend.dto.auth.ResendOtpRequest;
import com.masar.backend.dto.auth.VerifyOtpRequest;
import com.masar.backend.dto.user.RegisterRequest;
import com.masar.backend.dto.user.RegisterResponse;
import com.masar.backend.dto.user.UserDto;
import com.masar.backend.exception.AccountAlreadyVerifiedException;
import com.masar.backend.exception.EmailAlreadyExistsException;
import com.masar.backend.exception.EmailSendException;
import com.masar.backend.exception.InvalidOtpException;
import com.masar.backend.exception.TooManyRequestsException;
import com.masar.backend.exception.UnverifiedAccountException;
import com.masar.backend.model.User;
import com.masar.backend.repository.UserRepository;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

/**
 * Everything about getting into the app: register, login, verify OTP, resend OTP.
 */
@Service
public class AuthService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    /** How long a code is valid. */
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    /** Minimum time between two emails for the same user. */
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    /** Wrong guesses allowed before the code is locked. */
    private static final int MAX_ATTEMPTS = 5;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       OtpStore otpStore,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.otpStore = otpStore;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Creates a new account (verified=false), emails a verification code and returns
     * a short response. No token yet: the token comes after verifyOtp.
     * If the email belongs to a verified account, we reject it (409). If it belongs to
     * an account that is NOT verified yet, we replace its name, password and role with
     * the new request and send a new code: whoever proves they own the inbox by entering
     * the code owns the account. The 60 second resend cooldown still applies (429).
     * If the email fails to send, the account stays and the user can call resendOtp.
     */
    public RegisterResponse register(RegisterRequest request) {
        String email = request.getEmail().trim();

        Optional<User> existing = userRepository.findByEmail(email);
        User saved;

        if (existing.isPresent()) {
            User user = existing.get();
            if (user.isVerified()) {
                throw new EmailAlreadyExistsException(email);
            }

            // Not verified yet: wait for the cooldown, then overwrite the data with the new request
            if (!otpStore.tryStartCooldown(email, RESEND_COOLDOWN)) {
                throw new TooManyRequestsException(
                        "Please wait " + otpStore.cooldownSecondsLeft(email) + " seconds before requesting a new code");
            }
            user.setFullName(request.getFullName().trim());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole());
            saved = userRepository.save(user);
        } else {
            User user = request.toEntity(passwordEncoder.encode(request.getPassword()));
            saved = userRepository.save(user);
        }

        issueOtp(saved.getEmail());

        return new RegisterResponse(saved.getEmail(), "Verification code sent");
    }

    /**
     * Logs the user in: AuthenticationManager checks email and password (throws
     * BadCredentialsException if wrong). Unverified users are refused (403) after the
     * password check. Otherwise we generate a JWT and return it with the user.
     */
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim();

        // Throws BadCredentialsException if the email or password is wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isVerified()) {
            throw new UnverifiedAccountException();
        }

        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    /**
     * Checks the 6-digit code. On success the account becomes verified, the code is
     * deleted and the user gets a token (so the frontend can go straight to home).
     * Wrong guesses are counted in Redis (max 5), then the code is locked.
     */
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOtpException("Invalid code"));

        if (user.isVerified()) {
            throw new AccountAlreadyVerifiedException();
        }

        // Redis deletes the code after 5 minutes, so "not found" also covers "expired"
        String codeHash = otpStore.getHash(email)
                .orElseThrow(() -> new InvalidOtpException("Code expired or not found, please request a new one"));

        if (otpStore.getAttempts(email) >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Too many wrong attempts, please request a new code");
        }

        if (!passwordEncoder.matches(request.getOtp(), codeHash)) {
            otpStore.incrementAttempts(email);
            throw new InvalidOtpException("Invalid code");
        }

        user.setVerified(true);
        userRepository.save(user);
        otpStore.delete(email);

        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    /**
     * Sends a new code. Replaces the old one (the old code stops working) and resets
     * the attempts. Refuses if the last email was sent less than 60 seconds ago (429).
     * Unknown or already verified emails get the same answer and nothing is sent, so
     * this endpoint cannot be used to discover which emails are registered.
     */
    public RegisterResponse resendOtp(ResendOtpRequest request) {
        String email = request.getEmail().trim();
        RegisterResponse response = new RegisterResponse(email, "If the account exists, a code was sent");

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty() || user.get().isVerified()) {
            return response;
        }

        if (!otpStore.tryStartCooldown(email, RESEND_COOLDOWN)) {
            throw new TooManyRequestsException(
                    "Please wait " + otpStore.cooldownSecondsLeft(email) + " seconds before requesting a new code");
        }

        issueOtp(user.get().getEmail());
        return response;
    }

    /**
     * Creates a new code for this email, stores only its hash in Redis (replacing any old
     * code, expires after 5 minutes), then emails the plain code.
     * Throws EmailSendException if sending fails.
     */
    private void issueOtp(String email) {
        String otp = generateOtp();

        otpStore.save(email, passwordEncoder.encode(otp), OTP_TTL);
        otpStore.startCooldown(email, RESEND_COOLDOWN);

        try {
            emailService.sendOtp(email, otp);
        } catch (MailException e) {
            log.error("Could not send OTP email to {}", email, e);   // shows the real cause in the console
            otpStore.delete(email);   // nothing was delivered, so do not make the user wait
            throw new EmailSendException();
        }
    }

    /** Generates a 6-digit OTP as a String (leading zeros kept). */
    private String generateOtp() {
        int number = RANDOM.nextInt(1_000_000);  // 0 to 999999
        return String.format("%06d", number);  // 7 -> "000007"
    }
}
