package com.kuru.delivery.pricing.dto;

public class PricingRequest {
    private double weight;
    private double distance;
    private String serviceType;
    private String vehicleType; // e.g., "BIKE", "MOTORBIKE"
    private String dimensions; // Optional, e.g., "10x10x10"

    public PricingRequest() {}

    public PricingRequest(double weight, double distance, String serviceType, String dimensions) {
        this.weight = weight;
        this.distance = distance;
        this.serviceType = serviceType;
        this.vehicleType = vehicleType;
        this.dimensions = dimensions;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
}
