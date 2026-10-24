package com.masar.backend.controller;

import com.masar.backend.dto.job.JobRequest;
import com.masar.backend.dto.job.JobResponse;
import com.masar.backend.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * POST /api/jobs: the logged-in recruiter posts a job for their company (201).
     * Only RECRUITER can call it.
     */
    @PreAuthorize("hasRole('RECRUITER')")
    @PostMapping
    public ResponseEntity<JobResponse> createJob(Authentication auth,
                                                 @Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(auth.getName(), request));
    }
}
