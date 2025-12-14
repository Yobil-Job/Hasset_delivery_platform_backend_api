package com.kuru.delivery.pricing.dto;

import jakarta.validation.constraints.*;

public class PriceCalculationRequest {
    
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
    @DecimalMax(value = "1000.0", message = "Weight cannot exceed 1000 kg")
    private Double weight;
    
    // Distance is optional - can be calculated from coordinates
    @DecimalMin(value = "0.0", message = "Distance cannot be negative")
    @DecimalMax(value = "10000.0", message = "Distance cannot exceed 10000 km")
    private Double distance;
    
    // Coordinates (used if distance not provided)
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double pickupLat;
    
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double pickupLng;
    
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double deliveryLat;
    
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double deliveryLng;
    
    @Positive(message = "Service ID must be positive")
    private Long serviceId;
    
    public Double getWeight() {
        return weight;
    }
    
    public void setWeight(Double weight) {
        this.weight = weight;
    }
    
    public Double getDistance() {
        return distance;
    }
    
    public void setDistance(Double distance) {
        this.distance = distance;
    }
    
    public Double getPickupLat() {
        return pickupLat;
    }
    
    public void setPickupLat(Double pickupLat) {
        this.pickupLat = pickupLat;
    }
    
    public Double getPickupLng() {
        return pickupLng;
    }
    
    public void setPickupLng(Double pickupLng) {
        this.pickupLng = pickupLng;
    }
    
    public Double getDeliveryLat() {
        return deliveryLat;
    }
    
    public void setDeliveryLat(Double deliveryLat) {
        this.deliveryLat = deliveryLat;
    }
    
    public Double getDeliveryLng() {
        return deliveryLng;
    }
    
    public void setDeliveryLng(Double deliveryLng) {
        this.deliveryLng = deliveryLng;
    }
    
    public Long getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }
}

