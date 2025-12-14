package com.kuru.delivery.pricing.controller;

import com.kuru.delivery.common.util.RoleValidationUtil;
import com.kuru.delivery.pricing.model.PricingConfiguration;
import com.kuru.delivery.pricing.repository.PricingConfigRepository;
import com.kuru.delivery.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pricing")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPricingController {

    private final PricingConfigRepository repository;

    @Autowired
    private UserRepository userRepository;

    public AdminPricingController(PricingConfigRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/config")
    public ResponseEntity<List<PricingConfiguration>> getAllConfigs() {
        RoleValidationUtil.validateAdminRole(userRepository);
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/config")
    public ResponseEntity<PricingConfiguration> createConfig(@RequestBody PricingConfiguration config) {
        RoleValidationUtil.validateAdminRole(userRepository);
        // We might want to limit to only one config, but for now allow creation
        return ResponseEntity.ok(repository.save(config));
    }

    @PutMapping("/config/{id}")
    public ResponseEntity<PricingConfiguration> updateConfig(@PathVariable Long id, @RequestBody PricingConfiguration config) {
        RoleValidationUtil.validateAdminRole(userRepository);
        return repository.findById(id)
                .map(existing -> {
                    existing.setBaseFee(config.getBaseFee());
                    existing.setDistanceRatePerKm(config.getDistanceRatePerKm());
                    existing.setFreeWeightLimit(config.getFreeWeightLimit());
                    existing.setAdditionalWeightFeePerKg(config.getAdditionalWeightFeePerKg());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/config/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        RoleValidationUtil.validateAdminRole(userRepository);
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
