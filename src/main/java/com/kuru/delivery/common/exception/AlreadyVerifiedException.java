package com.kuru.delivery.common.exception;

public class AlreadyVerifiedException extends RuntimeException {
    public AlreadyVerifiedException(String email) {
        super("Email is already verified: " + email);
    }
}


