package com.kuru.delivery.order.dto;

import java.time.LocalDateTime;

public class UploadDeliveryProofResponse {
    private Long proofId;
    private String imageUrl;
    private LocalDateTime uploadedAt;

    public UploadDeliveryProofResponse() {}

    public UploadDeliveryProofResponse(Long proofId, String imageUrl, LocalDateTime uploadedAt) {
        this.proofId = proofId;
        this.imageUrl = imageUrl;
        this.uploadedAt = uploadedAt;
    }

    public Long getProofId() {
        return proofId;
    }

    public void setProofId(Long proofId) {
        this.proofId = proofId;
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
}

