package com.masar.backend.repository;

import com.masar.backend.model.SeekerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface SeekerProfileRepository extends JpaRepository<SeekerProfile, String> {
    // Spring Data reads UserEmail as profile.user.email. The entity graph loads the user in the same query.
    @EntityGraph(attributePaths = "user")
    Optional<SeekerProfile> findByUserEmail(String email);

    /**
     * Changes only the CV path (null removes it). A plain save() would write the whole
     * profile back and could overwrite an update made at the same time.
     */
    @Transactional
    @Modifying
    @Query("update SeekerProfile p set p.cvPath = :cvPath where p.userId = :userId")
    int updateCvPath(@Param("userId") String userId, @Param("cvPath") String cvPath);
}
