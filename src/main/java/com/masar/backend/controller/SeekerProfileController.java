package com.masar.backend.controller;

import com.masar.backend.dto.seeker.SeekerProfileRequest;
import com.masar.backend.dto.seeker.SeekerProfileResponse;
import com.masar.backend.service.SeekerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the logged-in job seeker's own profile. The email always comes from
 * the token (auth.getName()), never from the request. Access is limited to the
 * JOB_SEEKER role in SecurityConfig.
 */
@RestController
@RequestMapping("/api/seekers")
public class SeekerProfileController {

    private final SeekerProfileService seekerProfileService;

    public SeekerProfileController(SeekerProfileService seekerProfileService) {
        this.seekerProfileService = seekerProfileService;
    }

    /**
     * GET /api/seekers/me: returns the logged-in seeker's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<SeekerProfileResponse> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(seekerProfileService.getProfile(auth.getName()));
    }

    /**
     * PUT /api/seekers/me: creates or updates the logged-in seeker's profile.
     */
    @PutMapping("/me")
    public ResponseEntity<SeekerProfileResponse> updateMyProfile(Authentication auth,
                                                                 @Valid @RequestBody SeekerProfileRequest request) {
        return ResponseEntity.ok(seekerProfileService.updateProfile(auth.getName(), request));
    }
}
