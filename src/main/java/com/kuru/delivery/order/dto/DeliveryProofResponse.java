package com.kuru.delivery.order.dto;

import java.time.LocalDateTime;

public class DeliveryProofResponse {
    private Long id;
    private String imageUrl;
    private LocalDateTime uploadedAt;
    private String uploadedBy;

    public DeliveryProofResponse() {}

    public DeliveryProofResponse(Long id, String imageUrl, LocalDateTime uploadedAt, String uploadedBy) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.uploadedAt = uploadedAt;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}

