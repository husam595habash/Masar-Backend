package com.masar.backend.repository;

import com.masar.backend.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobIdAndSeekerId(Long jobId, String seekerId);
}
