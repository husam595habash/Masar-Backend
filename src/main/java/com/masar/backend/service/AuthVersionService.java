package com.masar.backend.service;

import com.masar.backend.model.User;
import com.masar.backend.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Keeps the latest authVersion of each user in the cache, so JwtAuthFilter
 * does not need the database on every request.
 * A token is valid only if its "ver" claim equals the value returned here.
 */
@Service
public class AuthVersionService {

    /** Returned when the user does not exist. It is never cached. */
    public static final int USER_NOT_FOUND = -1;

    private final UserRepository userRepository;

    public AuthVersionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Current authVersion of the user. First call reads the database and stores
     * the value in the "authVersion" cache, next calls are served from the cache.
     */
    @Cacheable(value = "authVersion", key = "#email", unless = "#result < 0")
    public int current(String email) {
        return userRepository.findByEmail(email)
                .map(User::getAuthVersion)
                .orElse(USER_NOT_FOUND);
    }

    /**
     * Removes the cached value of this user. Call it AFTER saving the new
     * authVersion in the database, the next current() call reloads it.
     */
    @CacheEvict(value = "authVersion", key = "#email")
    public void evict(String email) {
        // The work is done by the annotation
    }
}
