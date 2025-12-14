package com.kuru.delivery.admin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuru.delivery.common.util.RoleValidationUtil;
import com.kuru.delivery.driver.model.Driver;
import com.kuru.delivery.driver.service.DriverService;
import com.kuru.delivery.location.service.RedisLocationService;
import com.kuru.delivery.user.repository.UserRepository;

@RestController
@RequestMapping("/api/admin/drivers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDriverController {

    private final DriverService driverService;

    @Autowired
    private com.kuru.delivery.driver.repository.DriverRepository driverRepository;

    @Autowired
    private com.kuru.delivery.order.repository.OrderRepository orderRepository;

    @Autowired
    private RedisLocationService redisLocationService;

    @Autowired
    private UserRepository userRepository;

    public AdminDriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Driver>> getPendingDrivers() {
        RoleValidationUtil.validateAdminRole(userRepository);
        return ResponseEntity.ok(driverService.getPendingDrivers());
    }

    @GetMapping
    public ResponseEntity<List<Driver>> getAllDrivers() {
        RoleValidationUtil.validateAdminRole(userRepository);
        return ResponseEntity.ok(driverRepository.findAll());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<String> approveDriver(@PathVariable Long id) {
        RoleValidationUtil.validateAdminRole(userRepository);
        driverService.approveDriver(id);
        return ResponseEntity.ok("Driver approved successfully");
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<String> rejectDriver(@PathVariable Long id) {
        RoleValidationUtil.validateAdminRole(userRepository);
        driverService.rejectDriver(id);
        return ResponseEntity.ok("Driver rejected successfully");
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<String> suspendDriver(@PathVariable Long id) {
        RoleValidationUtil.validateAdminRole(userRepository);
        driverService.suspendDriver(id);
        return ResponseEntity.ok("Driver suspended successfully");
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveDrivers() {
        RoleValidationUtil.validateAdminRole(userRepository);
        List<Driver> allDrivers = driverRepository.findAll();
        java.util.List<java.util.Map<String, Object>> activeDrivers = new java.util.ArrayList<>();

        for (Driver driver : allDrivers) {
            List<com.kuru.delivery.order.model.Order> activeOrders = orderRepository.findByDriverAndStatusIn(driver, 
                List.of(com.kuru.delivery.order.model.OrderStatus.ASSIGNED, com.kuru.delivery.order.model.OrderStatus.PICKED_UP, com.kuru.delivery.order.model.OrderStatus.ON_THE_WAY));
            
            if (!activeOrders.isEmpty()) {
                com.kuru.delivery.order.model.Order currentOrder = activeOrders.get(0);
                Point point = redisLocationService.getDriverLocation(String.valueOf(driver.getId()));
                
                String locationStr = "";
                if (point != null) {
                    locationStr = point.getY() + "," + point.getX() + "," + System.currentTimeMillis();
                }
                
                java.util.Map<String, Object> driverInfo = new java.util.HashMap<>();
                driverInfo.put("driverId", driver.getId());
                driverInfo.put("driverName", driver.getUser().getFirstname() + " " + driver.getUser().getLastname());
                driverInfo.put("currentOrderId", currentOrder.getId());
                driverInfo.put("location", locationStr); // lat,lon,timestamp
                
                activeDrivers.add(driverInfo);
            }
        }

        return ResponseEntity.ok(activeDrivers);
    }
}
