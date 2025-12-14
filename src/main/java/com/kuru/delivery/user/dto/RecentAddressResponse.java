package com.kuru.delivery.user.dto;

import java.time.LocalDateTime;

public class RecentAddressResponse {
    private Long id;
    private String address;
    private double lat;
    private double lng;
    private LocalDateTime lastUsed;
    private String type;

    public RecentAddressResponse() {}

    public RecentAddressResponse(Long id, String address, double lat, double lng, LocalDateTime lastUsed, String type) {
        this.id = id;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.lastUsed = lastUsed;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

