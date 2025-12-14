package com.kuru.delivery.pricing.controller;

import com.kuru.delivery.common.util.RoleValidationUtil;
import com.kuru.delivery.pricing.model.ServiceOffering;
import com.kuru.delivery.pricing.repository.ServiceOfferingRepository;
import com.kuru.delivery.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pricing/services")
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceOfferingController {

    private final ServiceOfferingRepository repository;

    @Autowired
    private UserRepository userRepository;

    public AdminServiceOfferingController(ServiceOfferingRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<ServiceOffering>> getAllServices() {
        RoleValidationUtil.validateAdminRole(userRepository);
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<ServiceOffering> createService(@RequestBody ServiceOffering service) {
        RoleValidationUtil.validateAdminRole(userRepository);
        return ResponseEntity.ok(repository.save(service));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceOffering> updateService(@PathVariable Long id, @RequestBody ServiceOffering service) {
        RoleValidationUtil.validateAdminRole(userRepository);
        return repository.findById(id)
                .map(existing -> {
                    existing.setTitle(service.getTitle());
                    existing.setDescription(service.getDescription());
                    existing.setPriceText(service.getPriceText());
                    existing.setFeatures(service.getFeatures());
                    existing.setImageUrl(service.getImageUrl());
                    existing.setGradient(service.getGradient());
                    existing.setIcon(service.getIcon());
                    existing.setMultiplier(service.getMultiplier());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        RoleValidationUtil.validateAdminRole(userRepository);
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(409)
                    .body("Cannot delete service as it is being used by existing orders or configurations");
        }
    }
}
