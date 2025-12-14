package com.kuru.delivery.tracking.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuru.delivery.driver.model.Driver;
import com.kuru.delivery.driver.repository.DriverRepository;
import com.kuru.delivery.location.service.RedisLocationService;
import com.kuru.delivery.order.model.Order;
import com.kuru.delivery.order.model.OrderStatus;
import com.kuru.delivery.order.repository.OrderRepository;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.UserRepository;

@RestController
@RequestMapping("/api/driver/location")
public class DriverLocationController {

    @Autowired
    private RedisLocationService redisLocationService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;
    
    
    @PostMapping("/update")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> updateLocation(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Map<String, Double> location) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Driver profile not found"));

        // Check for active order
        List<Order> activeOrders = orderRepository.findByDriverAndStatusIn(driver, 
            List.of(OrderStatus.ASSIGNED, OrderStatus.PICKED_UP, OrderStatus.ON_THE_WAY));
        
        if (activeOrders.isEmpty()) {
            return ResponseEntity.badRequest().body("No active order found. Cannot update location.");
        }

        Double latitude = location.get("latitude");
        Double longitude = location.get("longitude");

        if (latitude == null || longitude == null) {
            return ResponseEntity.badRequest().body("Latitude and longitude are required");
        }

        redisLocationService.updateDriverLocation(String.valueOf(driver.getId()), latitude, longitude);
        return ResponseEntity.ok("Location updated");
    }
}
