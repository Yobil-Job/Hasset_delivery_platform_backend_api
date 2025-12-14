package com.kuru.delivery.common.exception;

/**
 * Exception thrown when a password does not meet security requirements.
 */
public class WeakPasswordException extends RuntimeException {
    
    public WeakPasswordException(String message) {
        super(message);
    }
}

