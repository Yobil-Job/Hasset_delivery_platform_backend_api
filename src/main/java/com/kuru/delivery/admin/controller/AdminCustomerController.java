package com.kuru.delivery.admin.controller;

import com.kuru.delivery.admin.dto.AdminCustomerResponse;
import com.kuru.delivery.common.util.RoleValidationUtil;
import com.kuru.delivery.order.model.Order;
import com.kuru.delivery.order.repository.OrderRepository;
import com.kuru.delivery.user.model.Role;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AdminCustomerResponse>> getAllCustomersWithStats() {
        // Double-check admin role in service layer
        RoleValidationUtil.validateAdminRole(userRepository);
        
        // Build stats map from all orders (may be empty if no orders yet)
        List<Order> allOrders = orderRepository.findAll();
        Map<Long, Stats> statsMap = new HashMap<>();

        for (Order order : allOrders) {
            Long customerId = order.getCustomerId();
            if (customerId == null) continue;

            Stats stats = statsMap.computeIfAbsent(customerId, id -> new Stats());
            stats.totalOrders++;
            if (order.getPrice() != null) {
                stats.totalSpent = stats.totalSpent.add(order.getPrice());
            }
            LocalDateTime created = order.getCreatedAt();
            if (created != null) {
                if (stats.lastOrderDate == null || created.isAfter(stats.lastOrderDate)) {
                    stats.lastOrderDate = created;
                }
            }
        }

        // Load all users with CUSTOMER role, even if they have zero orders
        List<User> customers = userRepository.findByRole(Role.CUSTOMER);
        List<AdminCustomerResponse> result = new ArrayList<>();

        for (User user : customers) {
            AdminCustomerResponse dto = new AdminCustomerResponse();
            dto.setId(user.getId());
            dto.setEmail(user.getEmail());
            dto.setFirstname(user.getFirstname());
            dto.setLastname(user.getLastname());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setCreatedAt(user.getCreatedAt());

            Stats stats = statsMap.get(user.getId());
            if (stats != null) {
                dto.setTotalOrders(stats.totalOrders);
                dto.setTotalSpent(stats.totalSpent);
                dto.setLastOrderDate(stats.lastOrderDate);
            } else {
                dto.setTotalOrders(0);
                dto.setTotalSpent(BigDecimal.ZERO);
                dto.setLastOrderDate(null);
            }

            result.add(dto);
        }

        // Optional: sort by most recent order first
        result.sort(Comparator.comparing(
                AdminCustomerResponse::getLastOrderDate,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return ResponseEntity.ok(result);
    }

    private static class Stats {
        long totalOrders = 0;
        BigDecimal totalSpent = BigDecimal.ZERO;
        LocalDateTime lastOrderDate = null;
    }
}


