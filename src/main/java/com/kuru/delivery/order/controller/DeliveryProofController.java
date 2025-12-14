package com.kuru.delivery.order.controller;

import com.kuru.delivery.order.dto.DeliveryProofResponse;
import com.kuru.delivery.order.dto.UploadDeliveryProofResponse;
import com.kuru.delivery.order.service.DeliveryProofService;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class DeliveryProofController {

    @Autowired
    private DeliveryProofService deliveryProofService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/{orderNumber}/delivery-proof")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> uploadProof(
            @PathVariable String orderNumber,
            @RequestParam("file") MultipartFile file) {
        try {
            Long driverUserId = getCurrentUserId();
            UploadDeliveryProofResponse response = deliveryProofService.uploadProof(orderNumber, driverUserId, file);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderNumber}/delivery-proof")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER', 'ADMIN')")
    public ResponseEntity<?> getProofs(@PathVariable String orderNumber) {
        try {
            Long userId = getCurrentUserId();
            
            // Check if user is admin
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            List<DeliveryProofResponse> proofs = deliveryProofService.getProofs(orderNumber, userId, isAdmin);
            return ResponseEntity.ok(Map.of("proofs", proofs));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private Long getCurrentUserId() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}

