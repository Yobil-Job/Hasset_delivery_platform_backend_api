package com.kuru.delivery.pricing.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class PricingPlan {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    private String name; 
    private String description;
    private double baseRate;

    public PricingPlan() {}

    public PricingPlan(String id, String name, String description, double baseRate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseRate = baseRate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getBaseRate() {
        return baseRate;
    }

    public void setBaseRate(double baseRate) {
        this.baseRate = baseRate;
    }
}
