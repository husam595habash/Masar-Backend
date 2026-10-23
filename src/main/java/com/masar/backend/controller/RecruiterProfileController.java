package com.masar.backend.controller;

import com.masar.backend.dto.recruiter.RecruiterProfileRequest;
import com.masar.backend.dto.recruiter.RecruiterProfileResponse;
import com.masar.backend.service.RecruiterProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the logged-in recruiter's own profile. The email always comes from
 * the token (auth.getName()), never from the request. Access is limited to the
 * RECRUITER role in SecurityConfig.
 */
@RestController
@RequestMapping("/api/recruiters")
public class RecruiterProfileController {

    private final RecruiterProfileService recruiterProfileService;

    public RecruiterProfileController(RecruiterProfileService recruiterProfileService) {
        this.recruiterProfileService = recruiterProfileService;
    }

    /**
     * GET /api/recruiters/me: returns the logged-in recruiter's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<RecruiterProfileResponse> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(recruiterProfileService.getProfile(auth.getName()));
    }

    /**
     * PUT /api/recruiters/me: creates or updates the logged-in recruiter's profile.
     */
    @PutMapping("/me")
    public ResponseEntity<RecruiterProfileResponse> updateMyProfile(Authentication auth,
                                                                    @Valid @RequestBody RecruiterProfileRequest request) {
        return ResponseEntity.ok(recruiterProfileService.updateProfile(auth.getName(), request));
    }
}
