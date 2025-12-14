package com.kuru.delivery.common.exception;

/**
 * Exception thrown when an account is locked due to too many failed login attempts.
 */
public class AccountLockedException extends RuntimeException {
    
    private final long lockoutRemainingSeconds;

    public AccountLockedException(String message, long lockoutRemainingSeconds) {
        super(message);
        this.lockoutRemainingSeconds = lockoutRemainingSeconds;
    }

    public long getLockoutRemainingSeconds() {
        return lockoutRemainingSeconds;
    }
}

