package com.kuru.delivery.payment.dto;

public class InitializePaymentResponse {
    private String checkoutUrl;
    private String txRef;

    public InitializePaymentResponse() {}

    public InitializePaymentResponse(String checkoutUrl, String txRef) {
        this.checkoutUrl = checkoutUrl;
        this.txRef = txRef;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getTxRef() {
        return txRef;
    }

    public void setTxRef(String txRef) {
        this.txRef = txRef;
    }
}

