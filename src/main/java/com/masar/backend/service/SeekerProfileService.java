package com.masar.backend.service;

import com.masar.backend.dto.seeker.SeekerProfileRequest;
import com.masar.backend.dto.seeker.SeekerProfileResponse;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.model.SeekerProfile;
import com.masar.backend.model.User;
import com.masar.backend.repository.SeekerProfileRepository;
import com.masar.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Everything a jobseeker does with their own profile.
 */
@Service
public class SeekerProfileService {
    private final SeekerProfileRepository seekerProfileRepository;
    private final UserRepository userRepository;

    public SeekerProfileService(SeekerProfileRepository seekerProfileRepository,
                                UserRepository userRepository) {
        this.seekerProfileRepository = seekerProfileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns the name, email and profile of this email (one query: the profile and its
     * user are loaded together). The profile is created empty when the account is
     * verified, so a missing one throws ResourceNotFoundException (404).
     */
    public SeekerProfileResponse getProfile(String email) {
        SeekerProfile profile = seekerProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        return SeekerProfileResponse.from(profile.getUser(), profile);
    }

    /**
     * Creates the profile if the seeker has none yet, otherwise updates it, and also
     * updates the user's full name. Both writes are one transaction (all or nothing).
     * Throws ResourceNotFoundException (404) if the user does not exist.
     */
    @Transactional
    public SeekerProfileResponse updateProfile(String email, SeekerProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SeekerProfile profile = seekerProfileRepository.findById(user.getId())
                .orElseGet(() -> SeekerProfile.builder().user(user).build());

        profile.setHeadline(request.getHeadline());
        profile.setLocation(request.getLocation());
        profile.setYearsExperience(request.getYearsExperience());

        seekerProfileRepository.save(profile);
        // Only the name column is written, so a password change made at the same time is never overwritten
        userRepository.updateFullName(user.getId(), request.getFullName().trim());

        return getProfile(email);
    }
}
