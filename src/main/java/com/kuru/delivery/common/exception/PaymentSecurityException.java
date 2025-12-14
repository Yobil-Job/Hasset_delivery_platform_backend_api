package com.kuru.delivery.common.exception;

/**
 * Exception thrown when payment security violations are detected.
 * Used for fraud attempts, unauthorized access, and payment manipulation.
 */
public class PaymentSecurityException extends RuntimeException {
    
    public PaymentSecurityException(String message) {
        super(message);
    }
    
    public PaymentSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}

