package com.kuru.delivery.pricing.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.kuru.delivery.pricing.model.PricingConfiguration;
import com.kuru.delivery.pricing.model.ServiceOffering;
import com.kuru.delivery.pricing.model.SubscriptionPlan;
import com.kuru.delivery.pricing.repository.PricingConfigRepository;
import com.kuru.delivery.pricing.repository.ServiceOfferingRepository;
import com.kuru.delivery.pricing.repository.SubscriptionPlanRepository;
import com.kuru.delivery.pricing.service.PricingService;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingConfigRepository configRepository;
    private final SubscriptionPlanRepository planRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final PricingService pricingService;

    public PricingController(PricingConfigRepository configRepository, 
                           SubscriptionPlanRepository planRepository,
                           ServiceOfferingRepository serviceRepository,
                           PricingService pricingService) {
        this.configRepository = configRepository;
        this.planRepository = planRepository;
        this.serviceRepository = serviceRepository;
        this.pricingService = pricingService;
    }

    @GetMapping("/config")
    public ResponseEntity<List<PricingConfiguration>> getConfig() {
        return ResponseEntity.ok(configRepository.findAll());
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getPlans() {
        return ResponseEntity.ok(planRepository.findAll());
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceOffering>> getServices() {
        return ResponseEntity.ok(serviceRepository.findAll());
    }

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculatePrice(@Valid @RequestBody com.kuru.delivery.pricing.dto.PriceCalculationRequest request) {
        try {
            // Validate weight
            if (request.getWeight() == null || request.getWeight() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Weight must be positive"));
            }
            if (request.getWeight() > 1000) {
                return ResponseEntity.badRequest().body(Map.of("error", "Weight cannot exceed 1000 kg"));
            }
            
            double distance;
            if (request.getDistance() != null && request.getDistance() > 0) {
                // Use provided distance but validate it
                distance = request.getDistance();
                if (distance > 10000) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Distance cannot exceed 10000 km"));
                }
            } else {
                // Calculate distance from coordinates
                if (request.getPickupLat() == null || request.getPickupLng() == null ||
                    request.getDeliveryLat() == null || request.getDeliveryLng() == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", 
                        "Either distance or all coordinates (pickupLat, pickupLng, deliveryLat, deliveryLng) must be provided"));
                }
                
                // Validate coordinates
                if (request.getPickupLat() < -90 || request.getPickupLat() > 90 ||
                    request.getDeliveryLat() < -90 || request.getDeliveryLat() > 90) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Latitude must be between -90 and 90"));
                }
                if (request.getPickupLng() < -180 || request.getPickupLng() > 180 ||
                    request.getDeliveryLng() < -180 || request.getDeliveryLng() > 180) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Longitude must be between -180 and 180"));
                }
                
                // CRITICAL: Always calculate distance on backend - never trust frontend
                distance = calculateDistance(
                    request.getPickupLat(), request.getPickupLng(),
                    request.getDeliveryLat(), request.getDeliveryLng()
                );
                
                // Validate calculated distance
                if (distance <= 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid distance calculated from coordinates"));
                }
                if (distance > 10000) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Calculated distance exceeds maximum limit"));
                }
            }

            // Validate serviceId if provided
            if (request.getServiceId() != null && request.getServiceId() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Service ID must be positive"));
            }

            // CRITICAL: Always calculate price on backend - never trust frontend
            Map<String, Object> result = pricingService.calculatePrice(
                request.getWeight(), 
                distance, 
                request.getServiceId()
            );
            
            // Validate result
            if (result == null || !result.containsKey("total")) {
                return ResponseEntity.status(500).body(Map.of("error", "Failed to calculate price"));
            }
            
            double total = (Double) result.get("total");
            if (total <= 0) {
                return ResponseEntity.status(500).body(Map.of("error", "Calculated price must be positive"));
            }
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
