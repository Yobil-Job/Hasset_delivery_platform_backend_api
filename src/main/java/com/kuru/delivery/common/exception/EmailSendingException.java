package com.kuru.delivery.common.exception;

/**
 * Exception thrown when email sending fails.
 * Provides user-friendly error messages.
 */
public class EmailSendingException extends RuntimeException {
    
    public EmailSendingException(String message) {
        super(message);
    }
    
    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}

