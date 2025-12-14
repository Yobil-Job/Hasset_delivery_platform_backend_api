package com.kuru.delivery.pricing.dto;

public class PricingResponse {
    private double basePrice;
    private double serviceFee;
    private double total;
    private String estimatedDelivery;

    public PricingResponse() {}

    public PricingResponse(double basePrice, double serviceFee, double total, String estimatedDelivery) {
        this.basePrice = basePrice;
        this.serviceFee = serviceFee;
        this.total = total;
        this.estimatedDelivery = estimatedDelivery;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(double serviceFee) {
        this.serviceFee = serviceFee;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(String estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }
}
