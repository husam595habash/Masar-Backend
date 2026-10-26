package com.masar.backend.repository;

import com.masar.backend.model.Application;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobIdAndSeekerId(Long jobId, String seekerId);

    // ApplicationResponse reads the job, its company and the seeker of every application.
    // The entity graph loads them in the same query, instead of 3 extra queries for each row (N+1).
    @EntityGraph(attributePaths = {"job", "job.company", "seeker"})
    List<Application> findBySeekerEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"job", "job.company", "seeker"})
    List<Application> findByJobIdOrderByCreatedAtDesc(Long jobId);
}
