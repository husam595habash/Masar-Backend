package com.masar.backend.infra;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Keeps verification codes in Redis.
 * <p>
 * Key "otp:{email}" is a hash with the fields "hash" (BCrypt hash of the code) and
 * "attempts" (wrong guesses). It expires by itself, so an expired code simply does not exist.
 * Key "otp:cooldown:{email}" exists only while the user must wait before asking for a new code.
 */
@Service
public class OtpStore {

    private static final String CODE_PREFIX = "otp:";
    private static final String COOLDOWN_PREFIX = "otp:cooldown:";

    private final StringRedisTemplate redis;

    public OtpStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Saves the code hash for this email with attempts = 0, replacing any old code.
     * Redis deletes it automatically after the ttl.
     */
    public void save(String email, String codeHash, Duration ttl) {
        String key = CODE_PREFIX + email;
        redis.opsForHash().putAll(key, Map.of("hash", codeHash, "attempts", "0"));
        redis.expire(key, ttl);
    }

    /**
     * Returns the stored hash, or empty if there is no code (never sent or expired).
     */
    public Optional<String> getHash(String email) {
        Object hash = redis.opsForHash().get(CODE_PREFIX + email, "hash");
        return Optional.ofNullable(hash).map(Object::toString);
    }

    /**
     * Returns how many wrong guesses were made on the current code.
     */
    public int getAttempts(String email) {
        Object attempts = redis.opsForHash().get(CODE_PREFIX + email, "attempts");
        return attempts == null ? 0 : Integer.parseInt(attempts.toString());
    }

    /**
     * Adds one wrong guess (atomic in Redis).
     */
    public void incrementAttempts(String email) {
        redis.opsForHash().increment(CODE_PREFIX + email, "attempts", 1);
    }

    /**
     * Deletes the code and the cooldown of this email.
     */
    public void delete(String email) {
        redis.delete(CODE_PREFIX + email);
        redis.delete(COOLDOWN_PREFIX + email);
    }

    /**
     * Starts the cooldown only if none is running (atomic).
     * Returns false if the user must still wait.
     */
    public boolean tryStartCooldown(String email, Duration cooldown) {
        Boolean started = redis.opsForValue().setIfAbsent(COOLDOWN_PREFIX + email, "1", cooldown);
        return Boolean.TRUE.equals(started);
    }

    /**
     * Seconds left before a new code may be requested (0 if none).
     */
    public long cooldownSecondsLeft(String email) {
        Long left = redis.getExpire(COOLDOWN_PREFIX + email, TimeUnit.SECONDS);
        return left == null || left < 0 ? 0 : left;
    }
}
