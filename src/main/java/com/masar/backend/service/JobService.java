package com.masar.backend.service;

import com.masar.backend.dto.job.JobRequest;
import com.masar.backend.dto.job.JobResponse;
import com.masar.backend.exception.BadRequestException;
import com.masar.backend.enums.JobStatus;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.model.Company;
import com.masar.backend.model.Job;
import com.masar.backend.model.RecruiterProfile;
import com.masar.backend.repository.JobRepository;
import com.masar.backend.repository.RecruiterProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    public JobService(JobRepository jobRepository, RecruiterProfileRepository recruiterProfileRepository) {
        this.jobRepository = jobRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    /**
     * Creates a job for the recruiter's company (status OPEN).
     * 404 if the recruiter has no profile, 400 if they have no company or min salary is above max salary.
     */
    @Transactional
    public JobResponse createJob(String email, JobRequest request) {
        RecruiterProfile profile = recruiterProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        Company company = profile.getCompany();
        if (company == null) {
            throw new BadRequestException("Create a company before posting a job");
        }

        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin() > request.getSalaryMax()) {
            throw new BadRequestException("Minimum salary cannot be above maximum salary");
        }

        Job job = Job.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .location(request.getLocation())
                .type(request.getType())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .company(company)
                .createdBy(profile.getUser())
                .build();

        return JobResponse.from(jobRepository.save(job));
    }

    /**
     * Returns all OPEN jobs, newest first.
     */
    public List<JobResponse> getOpenJobs() {
        return jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.OPEN).stream()
                .map(job -> JobResponse.from(job))
                .toList();
    }

    /**
     * Returns one job by id. 404 if it does not exist.
     */
    public JobResponse getJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        return JobResponse.from(job);
    }
}
