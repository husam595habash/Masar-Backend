package com.masar.backend.service;

import com.masar.backend.exception.EmailSendException;
import com.masar.backend.exception.InvalidOtpException;
import com.masar.backend.exception.TooManyRequestsException;
import com.masar.backend.infra.EmailService;
import com.masar.backend.infra.OtpStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String EMAIL = "ali@mail.com";

    @Mock private OtpStore otpStore;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OtpService otpService;

    @Nested
    @DisplayName("Issue OTP")
    class Issue {

        @Test
        @DisplayName("stores only the hash and emails the plain 6-digit code")
        void success() {
            when(otpStore.tryStartCooldown(EMAIL, Duration.ofSeconds(60))).thenReturn(true);
            when(passwordEncoder.encode(anyString())).thenReturn("HASH");

            otpService.issueOtp(EMAIL);

            ArgumentCaptor<String> plain = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendOtp(eq(EMAIL), plain.capture());
            assertThat(plain.getValue()).matches("\\d{6}");
            verify(passwordEncoder).encode(plain.getValue());
            verify(otpStore).save(EMAIL, "HASH", Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("cooldown running: throws 429 and does not touch the existing code")
        void cooldownRunning() {
            when(otpStore.tryStartCooldown(EMAIL, Duration.ofSeconds(60))).thenReturn(false);
            when(otpStore.cooldownSecondsLeft(EMAIL)).thenReturn(30L);

            assertThatThrownBy(() -> otpService.issueOtp(EMAIL)).isInstanceOf(TooManyRequestsException.class);

            verify(otpStore, never()).save(any(), any(), any());
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("email failure deletes the code and throws EmailSendException")
        void emailFails() {
            when(otpStore.tryStartCooldown(EMAIL, Duration.ofSeconds(60))).thenReturn(true);
            when(passwordEncoder.encode(anyString())).thenReturn("HASH");
            doThrow(new MailSendException("smtp down")).when(emailService).sendOtp(eq(EMAIL), anyString());

            assertThatThrownBy(() -> otpService.issueOtp(EMAIL)).isInstanceOf(EmailSendException.class);

            verify(otpStore).delete(EMAIL);
        }
    }

    @Nested
    @DisplayName("Verify OTP")
    class Verify {

        @Test
        @DisplayName("correct code passes and is not deleted yet")
        void success() {
            when(otpStore.getHash(EMAIL)).thenReturn(Optional.of("HASH"));
            when(otpStore.getAttempts(EMAIL)).thenReturn(0);
            when(passwordEncoder.matches("123456", "HASH")).thenReturn(true);

            assertThatCode(() -> otpService.verify(EMAIL, "123456")).doesNotThrowAnyException();

            verify(otpStore, never()).delete(any());
        }

        @Test
        @DisplayName("no stored code means expired or never sent")
        void expired() {
            when(otpStore.getHash(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpService.verify(EMAIL, "123456")).isInstanceOf(InvalidOtpException.class);
        }

        @Test
        @DisplayName("wrong code counts one attempt")
        void wrongCode() {
            when(otpStore.getHash(EMAIL)).thenReturn(Optional.of("HASH"));
            when(otpStore.getAttempts(EMAIL)).thenReturn(2);
            when(passwordEncoder.matches("000000", "HASH")).thenReturn(false);

            assertThatThrownBy(() -> otpService.verify(EMAIL, "000000")).isInstanceOf(InvalidOtpException.class);

            verify(otpStore).incrementAttempts(EMAIL);
        }

        @Test
        @DisplayName("after 5 wrong attempts the code is locked, even if the guess is right")
        void locked() {
            when(otpStore.getHash(EMAIL)).thenReturn(Optional.of("HASH"));
            when(otpStore.getAttempts(EMAIL)).thenReturn(5);

            assertThatThrownBy(() -> otpService.verify(EMAIL, "123456")).isInstanceOf(TooManyRequestsException.class);

            verify(passwordEncoder, never()).matches(any(), any());
        }
    }

    @Nested
    @DisplayName("Cooldown (checked first thing inside issueOtp)")
    class Cooldown {

        @Test
        @DisplayName("cooldown running: throws 429 with the seconds left, nothing is stored or sent")
        void running() {
            when(otpStore.tryStartCooldown(EMAIL, Duration.ofSeconds(60))).thenReturn(false);
            when(otpStore.cooldownSecondsLeft(EMAIL)).thenReturn(42L);

            assertThatThrownBy(() -> otpService.issueOtp(EMAIL))
                    .isInstanceOf(TooManyRequestsException.class)
                    .hasMessageContaining("42");

            verify(otpStore, never()).save(any(), any(), any());
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("no cooldown: the code is issued")
        void free() {
            when(otpStore.tryStartCooldown(EMAIL, Duration.ofSeconds(60))).thenReturn(true);
            when(passwordEncoder.encode(anyString())).thenReturn("HASH");

            assertThatCode(() -> otpService.issueOtp(EMAIL)).doesNotThrowAnyException();

            verify(emailService).sendOtp(eq(EMAIL), anyString());
        }
    }
}
