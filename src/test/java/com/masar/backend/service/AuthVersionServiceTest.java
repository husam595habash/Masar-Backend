package com.masar.backend.service;

import com.masar.backend.config.CacheConfig;
import com.masar.backend.model.User;
import com.masar.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the caching behavior of AuthVersionService.
 * The annotations (@Cacheable, @CacheEvict) only work through a Spring proxy, so this test
 * starts a tiny Spring context with an in-memory cache instead of Redis, and a fake repository.
 */
@SpringJUnitConfig(AuthVersionServiceTest.TestConfig.class)
class AuthVersionServiceTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheConfig.AUTH_VERSION);
        }

        @Bean
        AuthVersionService authVersionService(UserRepository userRepository) {
            return new AuthVersionService(userRepository);
        }
    }

    @Autowired
    private AuthVersionService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    // Same Spring context is reused by all tests, so clean the cache and the mock before each one.
    @BeforeEach
    void setUp() {
        Mockito.reset(userRepository);
        cacheManager.getCache(CacheConfig.AUTH_VERSION).clear();
    }

    private User userWithVersion(int version) {
        return User.builder().email("ali@mail.com").authVersion(version).build();
    }

    @Test
    @DisplayName("current: first call reads the database, second call is served from the cache")
    void current_isCached() {
        when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(userWithVersion(3)));

        int first = service.current("ali@mail.com");
        int second = service.current("ali@mail.com");

        assertThat(first).isEqualTo(3);
        assertThat(second).isEqualTo(3);
        verify(userRepository, times(1)).findByEmail("ali@mail.com");   // only one query
    }

    @Test
    @DisplayName("evict: the next call reloads the new value from the database")
    void evict_forcesReload() {
        when(userRepository.findByEmail("ali@mail.com"))
                .thenReturn(Optional.of(userWithVersion(3)))      // first read
                .thenReturn(Optional.of(userWithVersion(4)));     // after the password change

        assertThat(service.current("ali@mail.com")).isEqualTo(3);

        service.evict("ali@mail.com");

        assertThat(service.current("ali@mail.com")).isEqualTo(4);
        verify(userRepository, times(2)).findByEmail("ali@mail.com");
    }

    @Test
    @DisplayName("current: unknown user returns -1 and the result is not cached")
    void current_unknownUser_isNotCached() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThat(service.current("ghost@mail.com")).isEqualTo(AuthVersionService.USER_NOT_FOUND);
        assertThat(service.current("ghost@mail.com")).isEqualTo(AuthVersionService.USER_NOT_FOUND);

        verify(userRepository, times(2)).findByEmail("ghost@mail.com");   // asked twice: nothing was cached
    }

    @Test
    @DisplayName("current: each user has a separate cache entry")
    void current_perUser() {
        when(userRepository.findByEmail("ali@mail.com")).thenReturn(Optional.of(userWithVersion(3)));
        when(userRepository.findByEmail("sara@mail.com")).thenReturn(
                Optional.of(User.builder().email("sara@mail.com").authVersion(7).build()));

        assertThat(service.current("ali@mail.com")).isEqualTo(3);
        assertThat(service.current("sara@mail.com")).isEqualTo(7);
    }
}
