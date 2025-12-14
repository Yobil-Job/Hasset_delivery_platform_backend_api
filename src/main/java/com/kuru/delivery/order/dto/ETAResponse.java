package com.kuru.delivery.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kuru.delivery.order.model.OrderStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ETAResponse {
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private String estimatedArrival;
    private int estimatedMinutes;
    private OrderStatus currentStatus;
    private Location driverLocation;
    private Location destinationLocation;
    private double remainingDistanceKm;
    private double averageSpeedKmh;

    public ETAResponse() {}

    public String getEstimatedArrival() {
        return estimatedArrival;
    }

    public void setEstimatedArrival(String estimatedArrival) {
        this.estimatedArrival = estimatedArrival;
    }
    
    // Helper method to set from LocalDateTime
    public void setEstimatedArrivalFromLocalDateTime(LocalDateTime estimatedArrival) {
        if (estimatedArrival != null) {
            ZonedDateTime zonedDateTime = estimatedArrival.atZone(ZoneId.systemDefault());
            this.estimatedArrival = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).toString();
        } else {
            this.estimatedArrival = null;
        }
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(OrderStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Location getDriverLocation() {
        return driverLocation;
    }

    public void setDriverLocation(Location driverLocation) {
        this.driverLocation = driverLocation;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public double getRemainingDistanceKm() {
        return remainingDistanceKm;
    }

    public void setRemainingDistanceKm(double remainingDistanceKm) {
        this.remainingDistanceKm = remainingDistanceKm;
    }

    public double getAverageSpeedKmh() {
        return averageSpeedKmh;
    }

    public void setAverageSpeedKmh(double averageSpeedKmh) {
        this.averageSpeedKmh = averageSpeedKmh;
    }

    public static class Location {
        private double lat;
        private double lng;

        public Location() {}

        public Location(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
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
    }
}

