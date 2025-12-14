package com.kuru.delivery.driver.controller;

import com.kuru.delivery.auth.dto.RegisterRequest;
import com.kuru.delivery.common.dto.SuccessResponse;
import com.kuru.delivery.common.util.RoleValidationUtil;
import com.kuru.delivery.driver.model.Driver;
import com.kuru.delivery.driver.service.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;
    private final UserRepository userRepository;

    public DriverController(DriverService driverService, UserRepository userRepository) {
        this.driverService = driverService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDriver(@RequestBody RegisterRequest request) {
        try {
            driverService.registerDriver(request);
            return ResponseEntity.ok(new SuccessResponse("Driver registered successfully. Verification code sent to email. Please verify your email and wait for admin approval."));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Registration failed", "message", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred"));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> getCurrentDriver() {
        try {
            // Validate driver role from database
            RoleValidationUtil.validateRole(com.kuru.delivery.user.model.Role.DRIVER, userRepository);
            
            Long currentUserId = RoleValidationUtil.getCurrentUserId(userRepository);
            
            Optional<Driver> driverOpt = driverService.getDriverByUserId(currentUserId);
            if (driverOpt.isEmpty()) {
                return ResponseEntity.status(404).body(java.util.Map.of("error", "Driver profile not found"));
            }
            
            Driver driver = driverOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", driver.getId());
            response.put("vehicleType", driver.getVehicleType());
            response.put("licenseNumber", driver.getLicenseNumber());
            response.put("status", driver.getStatus());
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "Access denied", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to fetch driver profile", "message", e.getMessage()));
        }
    }
}
