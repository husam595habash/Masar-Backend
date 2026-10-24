package com.masar.backend.repository;

import com.masar.backend.enums.JobStatus;
import com.masar.backend.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);

    List<Job> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
