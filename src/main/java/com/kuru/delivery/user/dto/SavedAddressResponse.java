package com.kuru.delivery.user.dto;

import java.time.LocalDateTime;

public class SavedAddressResponse {
    private Long id;
    private String label;
    private String address;
    private double latitude;
    private double longitude;
    private boolean isDefault;
    private LocalDateTime createdAt;

    public SavedAddressResponse() {}

    public SavedAddressResponse(Long id, String label, String address, double latitude, double longitude, boolean isDefault, LocalDateTime createdAt) {
        this.id = id;
        this.label = label;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

