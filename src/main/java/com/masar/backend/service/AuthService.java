package com.masar.backend.service;

import com.masar.backend.security.JwtService;
import com.masar.backend.dto.auth.AuthResponse;
import com.masar.backend.dto.auth.LoginRequest;
import com.masar.backend.dto.auth.ResendOtpRequest;
import com.masar.backend.dto.auth.VerifyOtpRequest;
import com.masar.backend.dto.user.RegisterRequest;
import com.masar.backend.dto.user.RegisterResponse;
import com.masar.backend.dto.user.UserDto;
import com.masar.backend.exception.AccountAlreadyVerifiedException;
import com.masar.backend.exception.EmailAlreadyExistsException;
import com.masar.backend.exception.InvalidOtpException;
import com.masar.backend.exception.UnverifiedAccountException;
import com.masar.backend.model.User;
import com.masar.backend.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Everything about getting into the app: register, login, verify OTP, resend OTP.
 * The code rules themselves (expiry, cooldown, attempts) live in OtpService.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    /**
     * Creates a new account (verified=false), emails a verification code and returns
     * a short response. No token yet: the token comes after verifyOtp.
     * If the email belongs to a verified account, we reject it (409). If it belongs to
     * an account that is NOT verified yet, we replace its name, password and role with
     * the new request and send a new code: whoever proves they own the inbox by entering
     * the code owns the account. The 60 second resend cooldown still applies (429), and
     * it is checked before anything is changed, so a refused request leaves the account untouched.
     * New account + email fails: the account stays and the user can call resendOtp.
     * Existing unverified account + email fails: the old data stays (the new name,
     * password and role are NOT applied), the user must register again.
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

            // Send the code first: if the resend cooldown refuses (429), the account stays untouched
            otpService.issueOtp(user.getEmail());

            user.setFullName(request.getFullName().trim());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole());
            saved = userRepository.save(user);
        } else {
            User user = request.toEntity(passwordEncoder.encode(request.getPassword()));
            saved = userRepository.save(user);
            otpService.issueOtp(saved.getEmail());
        }

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
     */
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOtpException("Invalid code"));

        if (user.isVerified()) {
            throw new AccountAlreadyVerifiedException();
        }

        otpService.verify(email, request.getOtp());

        user.setVerified(true);
        userRepository.save(user);
        otpService.clear(email);

        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    /**
     * Sends a new code (the old one stops working). Refuses if the last email was sent
     * less than 60 seconds ago (429). Unknown or already verified emails get the same
     * answer and nothing is sent, so this endpoint cannot be used to discover which
     * emails are registered.
     */
    public RegisterResponse resendOtp(ResendOtpRequest request) {
        String email = request.getEmail().trim();
        RegisterResponse response = new RegisterResponse(email, "If the account exists, a code was sent");

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty() || user.get().isVerified()) {
            return response;
        }

        otpService.issueOtp(user.get().getEmail());
        return response;
    }

}
