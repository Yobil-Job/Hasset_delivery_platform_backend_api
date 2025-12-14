package com.kuru.delivery.user.controller;

import com.kuru.delivery.user.dto.AnalyticsResponse;
import com.kuru.delivery.user.dto.RecentAddressResponse;
import com.kuru.delivery.user.dto.SavedAddressRequest;
import com.kuru.delivery.user.dto.SavedAddressResponse;
import com.kuru.delivery.user.service.AnalyticsService;
import com.kuru.delivery.user.service.RecentAddressService;
import com.kuru.delivery.user.service.SavedAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.kuru.delivery.user.repository.UserRepository;
import com.kuru.delivery.user.model.User;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private RecentAddressService recentAddressService;

    @Autowired
    private SavedAddressService savedAddressService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/me/recent-addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<RecentAddressResponse>> getRecentAddresses(
            @RequestParam(required = false, defaultValue = "5") Integer limit) {
        Long userId = getCurrentUserId();
        List<RecentAddressResponse> addresses = recentAddressService.getRecentAddresses(userId, limit);
        return ResponseEntity.ok(addresses);
    }

    @PutMapping("/me/recent-addresses/{addressId}/update-last-used")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateLastUsed(@PathVariable Long addressId) {
        try {
            Long userId = getCurrentUserId();
            recentAddressService.updateLastUsed(addressId, userId);
            return ResponseEntity.ok(Map.of("message", "Last used timestamp updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Saved Addresses endpoints
    @GetMapping("/me/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<SavedAddressResponse>> getAddresses() {
        Long userId = getCurrentUserId();
        List<SavedAddressResponse> addresses = savedAddressService.getAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/me/addresses/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getAddress(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            SavedAddressResponse address = savedAddressService.getAddress(userId, id);
            return ResponseEntity.ok(address);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/me/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createAddress(@RequestBody SavedAddressRequest request) {
        try {
            Long userId = getCurrentUserId();
            SavedAddressResponse address = savedAddressService.createAddress(userId, request);
            return ResponseEntity.ok(address);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/me/addresses/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateAddress(@PathVariable Long id, @RequestBody SavedAddressRequest request) {
        try {
            Long userId = getCurrentUserId();
            SavedAddressResponse address = savedAddressService.updateAddress(userId, id, request);
            return ResponseEntity.ok(address);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/me/addresses/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> deleteAddress(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            savedAddressService.deleteAddress(userId, id);
            return ResponseEntity.ok(Map.of("message", "Address deleted successfully"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me/analytics")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @RequestParam(required = false, defaultValue = "month") String period) {
        Long userId = getCurrentUserId();
        AnalyticsResponse analytics = analyticsService.getCustomerAnalytics(userId, period);
        return ResponseEntity.ok(analytics);
    }

    private Long getCurrentUserId() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}

