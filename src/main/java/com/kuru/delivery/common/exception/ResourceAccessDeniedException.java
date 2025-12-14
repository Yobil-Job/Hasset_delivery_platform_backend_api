package com.kuru.delivery.common.exception;

/**
 * Exception thrown when a user attempts to access a resource they don't own.
 */
public class ResourceAccessDeniedException extends RuntimeException {
    
    public ResourceAccessDeniedException(String message) {
        super(message);
    }
}

