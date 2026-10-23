package com.masar.backend.service;

import com.masar.backend.dto.seeker.SeekerProfileRequest;
import com.masar.backend.dto.seeker.SeekerProfileResponse;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.model.SeekerProfile;
import com.masar.backend.model.User;
import com.masar.backend.repository.SeekerProfileRepository;
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
class SeekerProfileServiceTest {

    @Mock private SeekerProfileRepository seekerProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SeekerProfileService seekerProfileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("u-1")
                .fullName("Ali Hassan")
                .email("ali@mail.com")
                .build();
    }

    @Nested
    @DisplayName("Get profile")
    class GetProfile {

        @Test
        @DisplayName("returns name, email and the profile fields")
        void success() {
            SeekerProfile profile = SeekerProfile.builder()
                    .user(user).headline("Java dev").location("Ramallah").yearsExperience((short) 2).build();
            when(seekerProfileRepository.findByUserEmail("ali@mail.com")).thenReturn(Optional.of(profile));

            SeekerProfileResponse response = seekerProfileService.getProfile("ali@mail.com");

            assertThat(response.getFullName()).isEqualTo("Ali Hassan");
            assertThat(response.getEmail()).isEqualTo("ali@mail.com");
            assertThat(response.getHeadline()).isEqualTo("Java dev");
            assertThat(response.getLocation()).isEqualTo("Ramallah");
            assertThat(response.getYearsExperience()).isEqualTo((short) 2);
        }

        @Test
        @DisplayName("throws 404 when the profile does not exist")
        void notFound() {
            when(seekerProfileRepository.findByUserEmail("ali@mail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> seekerProfileService.getProfile("ali@mail.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update profile")
    class UpdateProfile {
        private SeekerProfileRequest request;

        @BeforeEach
        void setUp() {
            request = new SeekerProfileRequest();
            request.setFullName("  Ali Updated ");
            request.setHeadline("Backend dev");
            request.setLocation("Nablus");
            request.setYearsExperience((short) 3);
        }

        @Test
        @DisplayName("creates the profile when none exists and updates the trimmed name")
        void createsNewProfile() {
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));
            when(seekerProfileRepository.findById("u-1")).thenReturn(Optional.empty());
            // getProfile() runs at the end and reads the saved data
            SeekerProfile saved = SeekerProfile.builder().user(user).headline("Backend dev").build();
            when(seekerProfileRepository.findByUserEmail("ali@mail.com")).thenReturn(Optional.of(saved));

            SeekerProfileResponse response = seekerProfileService.updateProfile("ali@mail.com", request);

            ArgumentCaptor<SeekerProfile> captor = ArgumentCaptor.forClass(SeekerProfile.class);
            verify(seekerProfileRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isSameAs(user);
            assertThat(captor.getValue().getHeadline()).isEqualTo("Backend dev");
            assertThat(captor.getValue().getLocation()).isEqualTo("Nablus");
            assertThat(captor.getValue().getYearsExperience()).isEqualTo((short) 3);
            verify(userRepository).updateFullName("u-1", "Ali Updated");
            assertThat(response.getHeadline()).isEqualTo("Backend dev");
        }

        @Test
        @DisplayName("updates the existing profile instead of creating a new one")
        void updatesExistingProfile() {
            SeekerProfile existing = SeekerProfile.builder()
                    .user(user).headline("Old").location("Old").yearsExperience((short) 1).build();
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(user));
            when(seekerProfileRepository.findById("u-1")).thenReturn(Optional.of(existing));
            when(seekerProfileRepository.findByUserEmail("ali@mail.com")).thenReturn(Optional.of(existing));

            seekerProfileService.updateProfile("ali@mail.com", request);

            assertThat(existing.getHeadline()).isEqualTo("Backend dev");
            assertThat(existing.getLocation()).isEqualTo("Nablus");
            assertThat(existing.getYearsExperience()).isEqualTo((short) 3);
            verify(seekerProfileRepository).save(existing);
            verify(userRepository).updateFullName("u-1", "Ali Updated");
        }

        @Test
        @DisplayName("throws 404 and saves nothing when the user does not exist")
        void userNotFound() {
            when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> seekerProfileService.updateProfile("ali@mail.com", request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(seekerProfileRepository, never()).save(any());
            verify(userRepository, never()).updateFullName(any(), any());
        }
    }
}
