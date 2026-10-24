package com.masar.backend.controller;

import com.masar.backend.dto.job.JobRequest;
import com.masar.backend.dto.job.JobResponse;
import com.masar.backend.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /**
     * GET /api/jobs: all OPEN jobs, newest first (any logged-in user).
     */
    @GetMapping
    public ResponseEntity<List<JobResponse>> getOpenJobs() {
        return ResponseEntity.ok(jobService.getOpenJobs());
    }

    /**
     * GET /api/jobs/{id}: one job (any logged-in user). 404 if it does not exist.
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }
}
