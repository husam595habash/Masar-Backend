package com.masar.backend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * Turns on @Cacheable / @CacheEvict and decides how long each cache keeps its entries.
 * The cache names used in the code must match the names registered here.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Cache name used by AuthVersionService. */
    public static final String AUTH_VERSION = "authVersion";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default for any cache that has no specific setting
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                // authVersion entries live 1 hour, about the same as a token
                .withCacheConfiguration(AUTH_VERSION, defaults.entryTtl(Duration.ofHours(1)))
                .build();
    }
}
