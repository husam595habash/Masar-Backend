package com.masar.backend.repository;

import com.masar.backend.model.RecruiterProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, String> {
    // Spring Data reads UserEmail as profile.user.email. The entity graph loads the user in the same query.
    @EntityGraph(attributePaths = "user")
    Optional<RecruiterProfile> findByUserEmail(String email);
}
