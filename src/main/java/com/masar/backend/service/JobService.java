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
import org.springframework.security.access.AccessDeniedException;
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

        checkSalaryRange(request);

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

    /**
     * Returns all jobs (OPEN and CLOSED) of the recruiter's company, newest first.
     * Empty list if the recruiter has no company yet. 404 if the recruiter has no profile.
     */
    @Transactional(readOnly = true)
    public List<JobResponse> getMyJobs(String email) {
        RecruiterProfile profile = recruiterProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        if (profile.getCompany() == null) {
            return List.of();
        }

        return jobRepository.findByCompanyIdOrderByCreatedAtDesc(profile.getCompany().getId()).stream()
                .map(job -> JobResponse.from(job))
                .toList();
    }

    /**
     * Closes a job (status CLOSED), so seekers can no longer apply to it.
     * 404 if the job or the recruiter profile does not exist, 403 if the job belongs to another company.
     */
    @Transactional
    public JobResponse closeJob(String email, Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        checkJobBelongsToRecruiter(email, job);

        job.setStatus(JobStatus.CLOSED);
        return JobResponse.from(jobRepository.save(job));
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

    /**
     * Updates a job of the recruiter's company (status is not changed here, see closeJob).
     * 404 if the job or the recruiter profile does not exist, 403 if the job belongs to another company,
     * 400 if min salary is above max salary.
     */
    @Transactional
    public JobResponse updateJob(String email, Long jobId, JobRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        checkJobBelongsToRecruiter(email, job);
        checkSalaryRange(request);

        job.setTitle(request.getTitle().trim());
        job.setDescription(request.getDescription().trim());
        job.setLocation(request.getLocation());
        job.setType(request.getType());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());

        return JobResponse.from(jobRepository.save(job));
    }

    /**
     * 400 if both salaries are given and min is above max.
     */
    private void checkSalaryRange(JobRequest request) {
        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin() > request.getSalaryMax()) {
            throw new BadRequestException("Minimum salary cannot be above maximum salary");
        }
    }
}
