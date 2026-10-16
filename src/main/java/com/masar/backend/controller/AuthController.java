package com.masar.backend.controller;

import com.masar.backend.dto.auth.AuthResponse;
import com.masar.backend.dto.auth.LoginRequest;
import com.masar.backend.dto.auth.ResendOtpRequest;
import com.masar.backend.dto.auth.VerifyOtpRequest;
import com.masar.backend.dto.user.RegisterRequest;
import com.masar.backend.dto.user.RegisterResponse;
import com.masar.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;


    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.login(request));
    }

    /**
     * POST /api/auth/verify-otp
     * Checks the 6-digit code. If correct, verifies the account and returns the token.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.verifyOtp(request));
    }

    /**
     * POST /api/auth/resend-otp
     * Sends a new code (60 seconds between requests).
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<RegisterResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.resendOtp(request));
    }
}
