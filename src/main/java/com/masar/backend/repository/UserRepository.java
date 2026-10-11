package com.masar.backend.repository;

import com.masar.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);
}
