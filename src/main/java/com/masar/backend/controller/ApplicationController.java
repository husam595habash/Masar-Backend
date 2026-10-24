package com.masar.backend.controller;

import com.masar.backend.dto.application.ApplicationResponse;
import com.masar.backend.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * POST /api/jobs/{id}/applications: the logged-in seeker applies to a job (201).
     * Only JOB_SEEKER can call it.
     */
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @PostMapping("/api/jobs/{id}/applications")
    public ResponseEntity<ApplicationResponse> apply(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.apply(auth.getName(), id));
    }
}
