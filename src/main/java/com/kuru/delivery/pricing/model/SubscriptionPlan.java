package com.kuru.delivery.pricing.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String price;
    private String period;

    @ElementCollection
    private List<String> features;

    private String badge;
    private boolean popular;
    
    @Column(columnDefinition = "TEXT")
    private String gradient;
    
    private String icon; // Store icon name (e.g., "Zap", "TrendingUp")
    private double amount; // Numeric amount for payment

    public SubscriptionPlan() {}

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public boolean isPopular() { return popular; }
    public void setPopular(boolean popular) { this.popular = popular; }

    public String getGradient() { return gradient; }
    public void setGradient(String gradient) { this.gradient = gradient; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
