package com.masar.backend.repository;

import com.masar.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    /**
     * Changes only the CV path (null removes it). A plain save() would write the whole user
     * back, and a password change made while the upload was running could be overwritten.
     */
    @Transactional
    @Modifying
    @Query("update User u set u.cvPath = :cvPath where u.id = :id")
    int updateCvPath(@Param("id") String id, @Param("cvPath") String cvPath);

    /**
     * Changes only the full name (same reason as updateCvPath: never write the whole user back).
     * clearAutomatically makes the next read load the new name from the database.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.fullName = :fullName where u.id = :id")
    int updateFullName(@Param("id") String id, @Param("fullName") String fullName);
}
