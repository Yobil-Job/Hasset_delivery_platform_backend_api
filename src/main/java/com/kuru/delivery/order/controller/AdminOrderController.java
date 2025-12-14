package com.kuru.delivery.order.controller;

import com.kuru.delivery.order.model.Order;
import com.kuru.delivery.order.service.OrderAssignmentService;
import com.kuru.delivery.common.util.RoleValidationUtil;
import com.kuru.delivery.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    @Autowired
    private OrderAssignmentService orderAssignmentService;

    @Autowired
    private com.kuru.delivery.order.service.OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        // Double-check admin role in service layer
        RoleValidationUtil.validateAdminRole(userRepository);
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PostMapping("/{orderId}/assign-driver/{driverId}")
    public ResponseEntity<?> assignDriver(@PathVariable Long orderId, @PathVariable Long driverId) {
        // Double-check admin role in service layer
        RoleValidationUtil.validateAdminRole(userRepository);
        try {
            Order order = orderAssignmentService.assignDriverToOrder(orderId, driverId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
