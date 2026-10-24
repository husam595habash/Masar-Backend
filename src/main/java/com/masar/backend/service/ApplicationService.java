package com.masar.backend.service;

import com.masar.backend.dto.application.ApplicationResponse;
import com.masar.backend.enums.JobStatus;
import com.masar.backend.exception.BadRequestException;
import com.masar.backend.exception.ConflictException;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.model.Application;
import com.masar.backend.model.Job;
import com.masar.backend.model.User;
import com.masar.backend.repository.ApplicationRepository;
import com.masar.backend.repository.JobRepository;
import com.masar.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    /**
     * The seeker applies to a job (status PENDING).
     * 404 if the user or job does not exist, 400 if the job is closed,
     * 409 if the seeker already applied to this job.
     */
    @Transactional
    public ApplicationResponse apply(String email, Long jobId) {
        User seeker = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("This job is closed");
        }

        if (applicationRepository.existsByJobIdAndSeekerId(jobId, seeker.getId())) {
            throw new ConflictException("You already applied to this job");
        }

        Application application = Application.builder()
                .job(job)
                .seeker(seeker)
                .build();

        return ApplicationResponse.from(applicationRepository.save(application));
    }
}
