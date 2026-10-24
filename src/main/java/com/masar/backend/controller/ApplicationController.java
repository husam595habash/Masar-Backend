package com.masar.backend.controller;

import com.masar.backend.dto.application.ApplicationResponse;
import com.masar.backend.dto.application.ApplicationStatusRequest;
import com.masar.backend.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /**
     * GET /api/applications/mine: the logged-in seeker's applications, newest first.
     * Only JOB_SEEKER can call it.
     */
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @GetMapping("/api/applications/mine")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Authentication auth) {
        return ResponseEntity.ok(applicationService.getMyApplications(auth.getName()));
    }

    /**
     * GET /api/jobs/{id}/applications: who applied to a job of the recruiter's company.
     * Only RECRUITER can call it.
     */
    @PreAuthorize("hasRole('RECRUITER')")
    @GetMapping("/api/jobs/{id}/applications")
    public ResponseEntity<List<ApplicationResponse>> getJobApplications(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getJobApplications(auth.getName(), id));
    }

    /**
     * PATCH /api/applications/{id}: the recruiter accepts or rejects an application.
     * Only RECRUITER can call it. Body: { "status": "ACCEPTED" } or { "status": "REJECTED" }.
     */
    @PreAuthorize("hasRole('RECRUITER')")
    @PatchMapping("/api/applications/{id}")
    public ResponseEntity<ApplicationResponse> updateStatus(Authentication auth, @PathVariable Long id,
                                                            @Valid @RequestBody ApplicationStatusRequest request) {
        return ResponseEntity.ok(applicationService.updateStatus(auth.getName(), id, request.getStatus()));
    }
}
