package com.masar.backend.repository;

import com.masar.backend.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobIdAndSeekerId(Long jobId, String seekerId);

    List<Application> findBySeekerEmailOrderByCreatedAtDesc(String email);

    List<Application> findByJobIdOrderByCreatedAtDesc(Long jobId);
}
