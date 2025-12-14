package com.kuru.delivery.common.dto;

import java.time.Instant;

public class ErrorResponse {

    private int status;
    private String error;
    private Instant timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(int status, String error) {
        this.status = status;
        this.error = error;
        this.timestamp = Instant.now();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}


