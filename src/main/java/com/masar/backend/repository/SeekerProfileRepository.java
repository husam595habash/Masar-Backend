package com.masar.backend.repository;

import com.masar.backend.model.SeekerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeekerProfileRepository extends JpaRepository<SeekerProfile, String> {
    // Spring Data reads UserEmail as profile.user.email. The entity graph loads the user in the same query.
    @EntityGraph(attributePaths = "user")
    Optional<SeekerProfile> findByUserEmail(String email);
}
