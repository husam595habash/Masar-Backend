package com.masar.backend.service;

import com.masar.backend.dto.user.ChangePasswordRequest;
import com.masar.backend.exception.InvalidPasswordException;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.model.User;
import com.masar.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Runs this class with Mockito: it creates the @Mock objects before each test.
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // Fake objects: no real database, no real BCrypt. We control what they return.
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthVersionService authVersionService;

    // The real class we are testing. Mockito injects the two mocks above into its constructor.
    @InjectMocks
    private UserService userService;

    private User user;
    private ChangePasswordRequest request;

    // Runs before EVERY test, so each test starts with fresh data.
    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("ali@mail.com")
                .password("ENCODED_OLD")
                .build();

        request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass123");
        request.setNewPassword("newPass123");
    }

    @Test
    @DisplayName("changePassword: saves the new encoded password when everything is valid")
    void changePassword_success() {
        // Arrange: tell the mocks how to behave
        when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass123", "ENCODED_OLD")).thenReturn(true);   // current password is right
        when(passwordEncoder.matches("newPass123", "ENCODED_OLD")).thenReturn(false);  // new one is different
        when(passwordEncoder.encode("newPass123")).thenReturn("ENCODED_NEW");

        // Act: call the method under test
        userService.changePassword("ali@mail.com", request);

        // Assert: the user object now holds the new hash, and it was saved
        assertThat(user.getPassword()).isEqualTo("ENCODED_NEW");
        assertThat(user.getAuthVersion()).isEqualTo(1);              // 0 -> 1: old tokens are now invalid
        verify(userRepository).save(user);
        verify(authVersionService).evict("ali@mail.com");           // cached version is removed
    }

    @Test
    @DisplayName("changePassword: throws when the current password is wrong")
    void changePassword_wrongCurrentPassword() {
        when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass123", "ENCODED_OLD")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("ali@mail.com", request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Current password is incorrect");

        // Nothing must be saved and the password must stay the same
        verify(userRepository, never()).save(any());
        verify(authVersionService, never()).evict(any());
        assertThat(user.getPassword()).isEqualTo("ENCODED_OLD");
        assertThat(user.getAuthVersion()).isZero();
    }

    @Test
    @DisplayName("changePassword: throws when the new password equals the current one")
    void changePassword_sameAsCurrent() {
        request.setNewPassword("oldPass123");
        when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));
        // current and new are the same text, so matches() is true for both checks
        when(passwordEncoder.matches("oldPass123", "ENCODED_OLD")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword("ali@mail.com", request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("New password must be different from the current one");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword: throws when the user does not exist")
    void changePassword_userNotFound() {
        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword("ghost@mail.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, never()).save(any());
    }

}
