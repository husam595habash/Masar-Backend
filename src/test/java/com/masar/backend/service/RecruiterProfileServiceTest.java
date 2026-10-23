package com.masar.backend.service;

import com.masar.backend.dto.recruiter.RecruiterProfileRequest;
import com.masar.backend.dto.recruiter.RecruiterProfileResponse;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.model.RecruiterProfile;
import com.masar.backend.model.User;
import com.masar.backend.repository.RecruiterProfileRepository;
import com.masar.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruiterProfileServiceTest {

    @Mock private RecruiterProfileRepository recruiterProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RecruiterProfileService recruiterProfileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("u-1")
                .fullName("Sara Nasser")
                .email("sara@mail.com")
                .build();
    }

    @Nested
    @DisplayName("Get profile")
    class GetProfile {

        @Test
        @DisplayName("returns name, email and job title")
        void success() {
            RecruiterProfile profile = RecruiterProfile.builder().user(user).jobTitle("HR Manager").build();
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.of(profile));

            RecruiterProfileResponse response = recruiterProfileService.getProfile("sara@mail.com");

            assertThat(response.getFullName()).isEqualTo("Sara Nasser");
            assertThat(response.getEmail()).isEqualTo("sara@mail.com");
            assertThat(response.getJobTitle()).isEqualTo("HR Manager");
        }

        @Test
        @DisplayName("throws 404 when the profile does not exist")
        void notFound() {
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recruiterProfileService.getProfile("sara@mail.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update profile")
    class UpdateProfile {
        private RecruiterProfileRequest request;

        @BeforeEach
        void setUp() {
            request = new RecruiterProfileRequest();
            request.setFullName("  Sara Updated ");
            request.setJobTitle("Talent Lead");
        }

        @Test
        @DisplayName("creates the profile when none exists and updates the trimmed name")
        void createsNewProfile() {
            when(userRepository.findByEmail("sara@mail.com")).thenReturn(Optional.of(user));
            when(recruiterProfileRepository.findById("u-1")).thenReturn(Optional.empty());
            // getProfile() runs at the end and reads the saved data
            RecruiterProfile saved = RecruiterProfile.builder().user(user).jobTitle("Talent Lead").build();
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.of(saved));

            RecruiterProfileResponse response = recruiterProfileService.updateProfile("sara@mail.com", request);

            ArgumentCaptor<RecruiterProfile> captor = ArgumentCaptor.forClass(RecruiterProfile.class);
            verify(recruiterProfileRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isSameAs(user);
            assertThat(captor.getValue().getJobTitle()).isEqualTo("Talent Lead");
            verify(userRepository).updateFullName("u-1", "Sara Updated");
            assertThat(response.getJobTitle()).isEqualTo("Talent Lead");
        }

        @Test
        @DisplayName("updates the existing profile instead of creating a new one")
        void updatesExistingProfile() {
            RecruiterProfile existing = RecruiterProfile.builder().user(user).jobTitle("Old").build();
            when(userRepository.findByEmail("sara@mail.com")).thenReturn(Optional.of(user));
            when(recruiterProfileRepository.findById("u-1")).thenReturn(Optional.of(existing));
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.of(existing));

            recruiterProfileService.updateProfile("sara@mail.com", request);

            assertThat(existing.getJobTitle()).isEqualTo("Talent Lead");
            verify(recruiterProfileRepository).save(existing);
            verify(userRepository).updateFullName("u-1", "Sara Updated");
        }

        @Test
        @DisplayName("throws 404 and saves nothing when the user does not exist")
        void userNotFound() {
            when(userRepository.findByEmail("sara@mail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recruiterProfileService.updateProfile("sara@mail.com", request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(recruiterProfileRepository, never()).save(any());
            verify(userRepository, never()).updateFullName(any(), any());
        }
    }
}
