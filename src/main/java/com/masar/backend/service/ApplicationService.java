package com.masar.backend.service;

import com.masar.backend.dto.application.ApplicationResponse;
import com.masar.backend.enums.ApplicationStatus;
import com.masar.backend.enums.JobStatus;
import com.masar.backend.exception.BadRequestException;
import com.masar.backend.exception.ConflictException;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.model.Application;
import com.masar.backend.model.Job;
import com.masar.backend.model.RecruiterProfile;
import com.masar.backend.model.User;
import com.masar.backend.repository.ApplicationRepository;
import com.masar.backend.repository.JobRepository;
import com.masar.backend.repository.RecruiterProfileRepository;
import com.masar.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              UserRepository userRepository,
                              RecruiterProfileRepository recruiterProfileRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
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

    /**
     * Returns the logged-in seeker's applications, newest first.
     */
    public List<ApplicationResponse> getMyApplications(String email) {
        return applicationRepository.findBySeekerEmailOrderByCreatedAtDesc(email).stream()
                .map(application -> ApplicationResponse.from(application))
                .toList();
    }

    /**
     * Returns the applications of a job, newest first. Only the recruiters of the job's company can see them.
     * 404 if the job or the recruiter profile does not exist, 403 if the job belongs to another company.
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getJobApplications(String email, Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        checkJobBelongsToRecruiter(email, job);

        return applicationRepository.findByJobIdOrderByCreatedAtDesc(jobId).stream()
                .map(application -> ApplicationResponse.from(application))
                .toList();
    }

    /**
     * The recruiter accepts or rejects an application (not PENDING).
     * 404 if the application or profile does not exist, 400 if the status is PENDING,
     * 403 if the job belongs to another company.
     */
    @Transactional
    public ApplicationResponse updateStatus(String email, Long applicationId, ApplicationStatus status) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        checkJobBelongsToRecruiter(email, application.getJob());

        if (status == ApplicationStatus.PENDING) {
            throw new BadRequestException("Status must be ACCEPTED or REJECTED");
        }

        application.setStatus(status);
        return ApplicationResponse.from(applicationRepository.save(application));
    }

    /**
     * 403 unless the recruiter belongs to the company that owns the job.
     */
    private void checkJobBelongsToRecruiter(String email, Job job) {
        RecruiterProfile profile = recruiterProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        if (profile.getCompany() == null
                || !profile.getCompany().getId().equals(job.getCompany().getId())) {
            throw new AccessDeniedException("This job does not belong to your company");
        }
    }
}
