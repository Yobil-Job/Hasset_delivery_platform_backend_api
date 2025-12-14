package com.kuru.delivery.pricing.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "service_offerings")
public class ServiceOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String priceText; // e.g., "From 150 ETB"

    @ElementCollection
    private List<String> features;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;
    private String gradient;
    private String icon; // Store icon name
    private double multiplier = 0.0; // Service fee multiplier (e.g., 0.2 for 20%)

    public ServiceOffering() {}

    public double getMultiplier() { return multiplier; }
    public void setMultiplier(double multiplier) { this.multiplier = multiplier; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriceText() { return priceText; }
    public void setPriceText(String priceText) { this.priceText = priceText; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getGradient() { return gradient; }
    public void setGradient(String gradient) { this.gradient = gradient; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
