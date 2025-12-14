package com.kuru.delivery.auth.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for tracking login attempts and implementing account lockout.
 * Uses Redis for distributed rate limiting and account lockout.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 10;
    private static final long LOCKOUT_DURATION_MINUTES = 10;
    private static final String ATTEMPT_KEY_PREFIX = "login:attempts:";
    private static final String LOCKOUT_KEY_PREFIX = "login:lockout:";

    private final RedisTemplate<String, String> stringRedisTemplate;

    public LoginAttemptService(RedisTemplate<String, String> stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Records a failed login attempt for the given email.
     *
     * @param email the email address
     */
    public void recordFailedAttempt(String email) {
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        // Use string template for increment operations
        Long attempts = stringRedisTemplate.opsForValue().increment(attemptKey);
        
        if (attempts == 1) {
            // Set expiration on first attempt (reset window)
            stringRedisTemplate.expire(attemptKey, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
        }

        // If max attempts reached, lock the account
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            lockAccount(email);
        }
    }

    /**
     * Records a successful login and clears failed attempts.
     *
     * @param email the email address
     */
    public void recordSuccessfulLogin(String email) {
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        stringRedisTemplate.delete(attemptKey);
        
        // Also clear lockout if exists (in case of manual unlock)
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        stringRedisTemplate.delete(lockoutKey);
    }

    /**
     * Checks if the account is locked.
     *
     * @param email the email address
     * @return true if account is locked, false otherwise
     */
    public boolean isAccountLocked(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockoutKey));
    }

    /**
     * Gets the number of remaining attempts before lockout.
     *
     * @param email the email address
     * @return number of remaining attempts
     */
    public int getRemainingAttempts(String email) {
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        String attemptsStr = stringRedisTemplate.opsForValue().get(attemptKey);
        if (attemptsStr == null) {
            return MAX_ATTEMPTS;
        }
        try {
            Long attempts = Long.parseLong(attemptsStr);
            return Math.max(0, MAX_ATTEMPTS - attempts.intValue());
        } catch (NumberFormatException e) {
            return MAX_ATTEMPTS;
        }
    }

    /**
     * Gets the lockout expiration time in seconds.
     *
     * @param email the email address
     * @return seconds until lockout expires, or 0 if not locked
     */
    public long getLockoutRemainingSeconds(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        Long ttl = stringRedisTemplate.getExpire(lockoutKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * Locks the account for the specified duration.
     *
     * @param email the email address
     */
    private void lockAccount(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        stringRedisTemplate.opsForValue().set(lockoutKey, "locked");
        stringRedisTemplate.expire(lockoutKey, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
        
        // Clear attempt counter
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        stringRedisTemplate.delete(attemptKey);
    }
}

