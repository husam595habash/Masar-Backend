package com.masar.backend.service;

import com.masar.backend.dto.auth.AuthResponse;
import com.masar.backend.dto.auth.LoginRequest;
import com.masar.backend.enums.Role;
import com.masar.backend.exception.UnverifiedAccountException;
import com.masar.backend.infra.EmailService;
import com.masar.backend.infra.OtpStore;
import com.masar.backend.model.User;
import com.masar.backend.repository.UserRepository;
import com.masar.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpStore otpStore;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("Login")
    class Login {
        private LoginRequest loginRequest;
        private User user;

        @BeforeEach
        void setUp() {
            loginRequest = new LoginRequest();
            loginRequest.setEmail("ali@mail.com");
            loginRequest.setPassword("pass12345");

            user = User.builder()
                    .id("u-1")
                    .fullName("Ali")
                    .email("ali@mail.com")
                    .password("ENCODED")
                    .role(Role.JOB_SEEKER)
                    .verified(true)
                    .build();
        }

        @Test
        @DisplayName("verified user with correct credentials gets a token and user info")
        void successfulLogin() {
            when(authenticationManager.authenticate(any())).thenReturn(mock());
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("token");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getToken()).isEqualTo("token");
            assertThat(response.getUser().getEmail()).isEqualTo(loginRequest.getEmail());
            assertThat(response.getUser().getRole()).isEqualTo(Role.JOB_SEEKER);


        }

        private Authentication mock() {
            return org.mockito.Mockito.mock(Authentication.class);
        }
    }
}
