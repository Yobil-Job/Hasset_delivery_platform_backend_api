package com.kuru.delivery.pricing.service;

import com.kuru.delivery.pricing.model.PricingConfiguration;
import com.kuru.delivery.pricing.model.ServiceOffering;
import com.kuru.delivery.pricing.repository.PricingConfigRepository;
import com.kuru.delivery.pricing.repository.ServiceOfferingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class PricingService {

    private final PricingConfigRepository configRepository;
    private final ServiceOfferingRepository serviceRepository;

    public PricingService(PricingConfigRepository configRepository,
                         ServiceOfferingRepository serviceRepository) {
        this.configRepository = configRepository;
        this.serviceRepository = serviceRepository;
    }

    public Map<String, Object> calculatePrice(double weight, double distance, Long serviceId) {
        // Input validation (defense in depth)
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
        if (weight > 1000) {
            throw new IllegalArgumentException("Weight cannot exceed 1000 kg");
        }
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        if (distance > 10000) {
            throw new IllegalArgumentException("Distance cannot exceed 10000 km");
        }
        
        PricingConfiguration config = configRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Pricing configuration not found"));

        double baseFee = config.getBaseFee();
        double distanceCost = distance * config.getDistanceRatePerKm();
        
        double weightFee = 0;
        if (weight > config.getFreeWeightLimit()) {
            weightFee = (weight - config.getFreeWeightLimit()) * config.getAdditionalWeightFeePerKg();
        }

        double total = baseFee + distanceCost + weightFee;
        
        // Validate calculated values are positive
        if (baseFee < 0 || distanceCost < 0 || weightFee < 0 || total <= 0) {
            throw new IllegalStateException("Invalid price calculation result");
        }

        double serviceFee = 0;
        String serviceName = "Standard";
        
        if (serviceId != null) {
            ServiceOffering service = serviceRepository.findById(serviceId).orElse(null);
            if (service != null) {
                serviceFee = total * service.getMultiplier();
                serviceName = service.getTitle();
            }
        }

        total += serviceFee;

        Map<String, Object> result = new HashMap<>();
        result.put("baseFee", baseFee);
        result.put("distanceCost", distanceCost);
        result.put("weightFee", weightFee);
        result.put("serviceFee", serviceFee);
        result.put("serviceName", serviceName);
        result.put("total", total);
        
        return result;
    }

    public BigDecimal calculateTotalPrice(double weight, double distance, Long serviceId) {
        Map<String, Object> calculation = calculatePrice(weight, distance, serviceId);
        double total = (double) calculation.get("total");
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }
}
