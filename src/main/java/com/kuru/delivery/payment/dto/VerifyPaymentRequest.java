package com.kuru.delivery.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyPaymentRequest {
    @NotBlank(message = "Transaction reference is required")
    private String txRef;

    public String getTxRef() {
        return txRef;
    }

    public void setTxRef(String txRef) {
        this.txRef = txRef;
    }
}

