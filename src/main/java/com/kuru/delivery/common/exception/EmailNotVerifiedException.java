package com.kuru.delivery.common.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String email) {
        super("Email address " + email + " has not been verified. Please check your email for the verification code.");
    }
}

