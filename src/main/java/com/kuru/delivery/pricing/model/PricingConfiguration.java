package com.kuru.delivery.pricing.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pricing_configurations")
public class PricingConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private double baseFee;
    
    private double distanceRatePerKm;
    
    private double freeWeightLimit;
    
    private double additionalWeightFeePerKg;

    public PricingConfiguration() {}

    public PricingConfiguration(double baseFee, double distanceRatePerKm, double freeWeightLimit, double additionalWeightFeePerKg) {
        this.baseFee = baseFee;
        this.distanceRatePerKm = distanceRatePerKm;
        this.freeWeightLimit = freeWeightLimit;
        this.additionalWeightFeePerKg = additionalWeightFeePerKg;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(double baseFee) {
        this.baseFee = baseFee;
    }

    public double getDistanceRatePerKm() {
        return distanceRatePerKm;
    }

    public void setDistanceRatePerKm(double distanceRatePerKm) {
        this.distanceRatePerKm = distanceRatePerKm;
    }

    public double getFreeWeightLimit() {
        return freeWeightLimit;
    }

    public void setFreeWeightLimit(double freeWeightLimit) {
        this.freeWeightLimit = freeWeightLimit;
    }

    public double getAdditionalWeightFeePerKg() {
        return additionalWeightFeePerKg;
    }

    public void setAdditionalWeightFeePerKg(double additionalWeightFeePerKg) {
        this.additionalWeightFeePerKg = additionalWeightFeePerKg;
    }
}
