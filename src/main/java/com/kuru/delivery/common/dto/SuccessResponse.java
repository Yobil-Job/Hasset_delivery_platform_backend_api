package com.kuru.delivery.common.dto;

import java.time.Instant;

public class SuccessResponse {

    private String message;
    private Instant timestamp;

    public SuccessResponse() {
    }

    public SuccessResponse(String message) {
        this.message = message;
        this.timestamp = Instant.now();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}


