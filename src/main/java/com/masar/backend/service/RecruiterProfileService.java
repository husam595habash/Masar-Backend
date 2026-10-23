package com.masar.backend.service;

import com.masar.backend.dto.recruiter.RecruiterProfileRequest;
import com.masar.backend.dto.recruiter.RecruiterProfileResponse;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.model.RecruiterProfile;
import com.masar.backend.model.User;
import com.masar.backend.repository.RecruiterProfileRepository;
import com.masar.backend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RecruiterProfileService {
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final UserRepository userRepository;

    public RecruiterProfileService(RecruiterProfileRepository recruiterProfileRepository, UserRepository userRepository) {
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.userRepository = userRepository;
    }


    public RecruiterProfileResponse getProfile(String email){
        RecruiterProfile profile = recruiterProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        return RecruiterProfileResponse.from(profile.getUser(), profile);
    }



    @Transactional
    public RecruiterProfileResponse updateProfile(String email, RecruiterProfileRequest request){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RecruiterProfile profile = recruiterProfileRepository.findById(user.getId())
                .orElseGet(() -> RecruiterProfile.builder().user(user).build());

        profile.setJobTitle(request.getJobTitle());

        recruiterProfileRepository.save(profile);

        userRepository.updateFullName(user.getId(), request.getFullName().trim());

        return getProfile(email);
    }
}
