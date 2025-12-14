package com.kuru.delivery.pricing.controller;

import com.kuru.delivery.common.util.RoleValidationUtil;
import com.kuru.delivery.pricing.model.SubscriptionPlan;
import com.kuru.delivery.pricing.repository.SubscriptionPlanRepository;
import com.kuru.delivery.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pricing/plans")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubscriptionPlanController {

    private final SubscriptionPlanRepository repository;

    @Autowired
    private UserRepository userRepository;

    public AdminSubscriptionPlanController(SubscriptionPlanRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        RoleValidationUtil.validateAdminRole(userRepository);
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<SubscriptionPlan> createPlan(@RequestBody SubscriptionPlan plan) {
        RoleValidationUtil.validateAdminRole(userRepository);
        return ResponseEntity.ok(repository.save(plan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionPlan> updatePlan(@PathVariable Long id, @RequestBody SubscriptionPlan plan) {
        RoleValidationUtil.validateAdminRole(userRepository);
        return repository.findById(id)
                .map(existing -> {
                    existing.setName(plan.getName());
                    existing.setDescription(plan.getDescription());
                    existing.setPrice(plan.getPrice());
                    existing.setPeriod(plan.getPeriod());
                    existing.setFeatures(plan.getFeatures());
                    existing.setBadge(plan.getBadge());
                    existing.setPopular(plan.isPopular());
                    existing.setGradient(plan.getGradient());
                    existing.setIcon(plan.getIcon());
                    existing.setAmount(plan.getAmount());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable Long id) {
        RoleValidationUtil.validateAdminRole(userRepository);
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(409)
                    .body("Cannot delete plan as it is being used");
        }
    }
}
