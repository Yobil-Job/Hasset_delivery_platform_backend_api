package com.kuru.delivery.auth.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing JWT token blacklist.
 * Stores revoked tokens in Redis until they expire naturally.
 */
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";
    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Adds a token to the blacklist.
     * The token will be stored until its natural expiration time.
     *
     * @param token the JWT token to blacklist
     * @param expirationTimeSeconds time until token expires (in seconds)
     */
    public void blacklistToken(String token, long expirationTimeSeconds) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        String key = BLACKLIST_KEY_PREFIX + token;
        // Store token with expiration matching JWT expiration
        // Add small buffer (60 seconds) to ensure it's blacklisted slightly longer than JWT expiry
        redisTemplate.opsForValue().set(key, "blacklisted");
        redisTemplate.expire(key, expirationTimeSeconds + 60, TimeUnit.SECONDS);
    }

    /**
     * Checks if a token is blacklisted.
     *
     * @param token the JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        String key = BLACKLIST_KEY_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Removes a token from the blacklist (for manual cleanup if needed).
     *
     * @param token the JWT token to remove
     */
    public void removeFromBlacklist(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        String key = BLACKLIST_KEY_PREFIX + token;
        redisTemplate.delete(key);
    }
}

