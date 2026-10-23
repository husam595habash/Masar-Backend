package com.masar.backend.service;

import com.masar.backend.dto.auth.AuthResponse;
import com.masar.backend.dto.auth.LoginRequest;
import com.masar.backend.dto.auth.ResendOtpRequest;
import com.masar.backend.dto.auth.VerifyOtpRequest;
import com.masar.backend.dto.user.RegisterRequest;
import com.masar.backend.dto.user.RegisterResponse;
import com.masar.backend.enums.Role;
import com.masar.backend.exception.AccountAlreadyVerifiedException;
import com.masar.backend.exception.EmailAlreadyExistsException;
import com.masar.backend.exception.InvalidOtpException;
import com.masar.backend.exception.TooManyRequestsException;
import com.masar.backend.exception.UnverifiedAccountException;
import com.masar.backend.dto.user.RegisterRequest;
import com.masar.backend.exception.EmailAlreadyExistsException;
import com.masar.backend.model.RecruiterProfile;
import com.masar.backend.model.SeekerProfile;
import com.masar.backend.model.User;
import com.masar.backend.repository.RecruiterProfileRepository;
import com.masar.backend.repository.SeekerProfileRepository;
import com.masar.backend.repository.UserRepository;
import com.masar.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpService otpService;
    @Mock private SeekerProfileRepository seekerProfileRepository;
    @Mock private RecruiterProfileRepository recruiterProfileRepository;

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

    @Nested
    @DisplayName("Register")
    class Register {
        private RegisterRequest request;

        @BeforeEach
        void setUp() {
            request = new RegisterRequest();
            request.setFullName("  Ali Hassan ");
            request.setEmail("ali@mail.com");
            request.setPassword("newPass123");
            request.setRole(Role.JOB_SEEKER);
        }

        @Test
        @DisplayName("new email: account is saved as unverified, then the code is issued")
        void newAccount() {
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("newPass123")).thenReturn("ENCODED");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            RegisterResponse response = authService.register(request);

            assertThat(response.getEmail()).isEqualTo("ali@mail.com");
            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            InOrder order = inOrder(userRepository, otpService);
            order.verify(userRepository).save(saved.capture());
            order.verify(otpService).issueOtp("ali@mail.com");
            assertThat(saved.getValue().isVerified()).isFalse();
            assertThat(saved.getValue().getFullName()).isEqualTo("Ali Hassan");
        }

        @Test
        @DisplayName("verified email is rejected and no code is sent")
        void verifiedEmail() {
            User verified = User.builder().email("ali@mail.com").role(Role.JOB_SEEKER).verified(true).build();
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(verified));

            assertThatThrownBy(() -> authService.register(request)).isInstanceOf(EmailAlreadyExistsException.class);

            verifyNoInteractions(otpService);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("unverified email: data is overwritten only after the code was issued")
        void unverifiedOverwrite() {
            User old = User.builder().fullName("Old").email("ali@mail.com").password("OLD")
                    .role(Role.RECRUITER).verified(false).build();
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(old));
            when(passwordEncoder.encode("newPass123")).thenReturn("ENCODED");
            when(userRepository.save(old)).thenReturn(old);

            authService.register(request);

            InOrder order = inOrder(otpService, userRepository);
            order.verify(otpService).issueOtp("ali@mail.com");
            order.verify(userRepository).save(old);
            assertThat(old.getFullName()).isEqualTo("Ali Hassan");
            assertThat(old.getPassword()).isEqualTo("ENCODED");
            assertThat(old.getRole()).isEqualTo(Role.JOB_SEEKER);
        }

        @Test
        @DisplayName("unverified email during cooldown (429): the account stays untouched")
        void cooldownLeavesAccountUntouched() {
            User old = User.builder().fullName("Old").email("ali@mail.com").password("OLD")
                    .role(Role.RECRUITER).verified(false).build();
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(old));
            doThrow(new TooManyRequestsException("wait")).when(otpService).issueOtp("ali@mail.com");

            assertThatThrownBy(() -> authService.register(request)).isInstanceOf(TooManyRequestsException.class);

            verify(userRepository, never()).save(any());
            assertThat(old.getFullName()).isEqualTo("Old");
            assertThat(old.getPassword()).isEqualTo("OLD");
            assertThat(old.getRole()).isEqualTo(Role.RECRUITER);
        }
    }

    @Nested
    @DisplayName("Verify OTP")
    class VerifyOtp {
        private VerifyOtpRequest request;
        private User user;

        @BeforeEach
        void setUp() {
            request = new VerifyOtpRequest();
            request.setEmail("ali@mail.com");
            request.setOtp("123456");

            user = User.builder()
                    .id("u-1")
                    .fullName("Ali")
                    .email("ali@mail.com")
                    .password("ENCODED")
                    .role(Role.JOB_SEEKER)
                    .verified(false)
                    .build();
        }

        @Test
        @DisplayName("correct code verifies the account, clears the code and returns a token")
        void success() {
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("token");

            AuthResponse response = authService.verifyOtp(request);

            assertThat(user.isVerified()).isTrue();
            assertThat(response.getToken()).isEqualTo("token");
            verify(userRepository).save(user);
            verify(seekerProfileRepository).save(any(SeekerProfile.class));
            verify(recruiterProfileRepository, never()).save(any());
            verify(otpService).clear("ali@mail.com");
        }

        @Test
        @DisplayName("recruiter gets a recruiter profile and no seeker profile")
        void recruiterGetsRecruiterProfile() {
            user.setRole(Role.RECRUITER);
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("token");

            authService.verifyOtp(request);

            verify(recruiterProfileRepository).save(any(RecruiterProfile.class));
            verify(seekerProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("wrong code leaves the account unverified and keeps the code")
        void wrongCode() {
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));
            doThrow(new InvalidOtpException("Invalid code")).when(otpService).verify("ali@mail.com", "123456");

            assertThatThrownBy(() -> authService.verifyOtp(request)).isInstanceOf(InvalidOtpException.class);

            assertThat(user.isVerified()).isFalse();
            verify(userRepository, never()).save(any());
            verify(otpService, never()).clear(any());
        }

        @Test
        @DisplayName("unknown email gets the generic invalid-code answer")
        void unknownEmail() {
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyOtp(request)).isInstanceOf(InvalidOtpException.class);
            verifyNoInteractions(otpService);
        }

        @Test
        @DisplayName("already verified account is rejected without checking the code")
        void alreadyVerified() {
            user.setVerified(true);
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyOtp(request)).isInstanceOf(AccountAlreadyVerifiedException.class);
            verifyNoInteractions(otpService);
        }
    }

    @Nested
    @DisplayName("Resend OTP")
    class ResendOtp {
        private ResendOtpRequest request;

        @BeforeEach
        void setUp() {
            request = new ResendOtpRequest();
            request.setEmail("ali@mail.com");
        }

        @Test
        @DisplayName("unverified user gets a new code")
        void success() {
            User user = User.builder().email("ali@mail.com").role(Role.JOB_SEEKER).verified(false).build();
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));

            authService.resendOtp(request);

            verify(otpService).issueOtp("ali@mail.com");
        }

        @Test
        @DisplayName("unknown email gets the same answer and nothing is sent")
        void unknownEmail() {
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.empty());

            RegisterResponse response = authService.resendOtp(request);

            assertThat(response.getMessage()).isEqualTo("If the account exists, a code was sent");
            verifyNoInteractions(otpService);
        }

        @Test
        @DisplayName("cooldown still running: no new code is sent")
        void cooldown() {
            User user = User.builder().email("ali@mail.com").role(Role.JOB_SEEKER).verified(false).build();
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));
            doThrow(new TooManyRequestsException("wait")).when(otpService).issueOtp("ali@mail.com");

            assertThatThrownBy(() -> authService.resendOtp(request)).isInstanceOf(TooManyRequestsException.class);
        }
    }
}
