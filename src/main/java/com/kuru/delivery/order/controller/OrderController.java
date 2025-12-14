package com.kuru.delivery.order.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kuru.delivery.driver.model.Driver;
import com.kuru.delivery.driver.repository.DriverRepository;
import com.kuru.delivery.location.service.RedisLocationService;
import com.kuru.delivery.order.dto.CreateOrderRequest;
import com.kuru.delivery.order.dto.OrderResponse;
import com.kuru.delivery.order.model.Order;
import com.kuru.delivery.order.model.OrderStatus;
import com.kuru.delivery.order.service.OrderService;
import com.kuru.delivery.user.model.User;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final com.kuru.delivery.user.repository.UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderController(OrderService orderService, com.kuru.delivery.user.repository.UserRepository userRepository,
    		DriverRepository driverRepository, SimpMessagingTemplate messagingTemplate) {
        this.orderService = orderService;
        this.userRepository = userRepository;
       this.driverRepository = driverRepository;
       this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            Long customerId = getCurrentUserId();
            OrderResponse response = orderService.createOrder(request, customerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Long serviceId) {
        Long customerId = getCurrentUserId();
        List<OrderResponse> orders = orderService.getOrdersForCustomerWithFilters(
                customerId, status, startDate, endDate, minPrice, maxPrice, serviceId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getOrder(@PathVariable String orderNumber) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            OrderResponse order;
            if (isAdmin) {
                // Admin can access any order
                order = orderService.getOrderByOrderNumber(orderNumber);
            } else {
                // Customer can only access their own orders
                Long customerId = getCurrentUserId();
                order = orderService.getOrderByOrderNumberForUser(orderNumber, customerId);
            }
            
            return ResponseEntity.ok(order);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (com.kuru.delivery.common.exception.ResourceAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{orderNumber}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderNumber) {
        try {
            Long customerId = getCurrentUserId();
            OrderResponse response = orderService.cancelOrder(orderNumber, customerId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (com.kuru.delivery.common.exception.ResourceAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{orderNumber}/status")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<?> updateOrderStatus(@PathVariable String orderNumber, @RequestBody Map<String, String> statusUpdate) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            // If not admin, validate driver owns the order
            if (!isAdmin) {
                Long driverUserId = getCurrentUserId();
                Order order = orderRepository.findByOrderNumber(orderNumber)
                        .orElseThrow(() -> new EntityNotFoundException("Order not found"));
                
                // Validate driver owns this order
                if (order.getDriver() == null || order.getDriver().getUser() == null ||
                    !order.getDriver().getUser().getId().equals(driverUserId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "You can only update status for orders assigned to you"));
                }
                
                // Validate status transition is allowed for drivers
                String statusStr = statusUpdate.get("status");
                OrderStatus newStatus = OrderStatus.valueOf(statusStr);
                validateDriverStatusTransition(order.getStatus(), newStatus);
            }
            
            String statusStr = statusUpdate.get("status");
            com.kuru.delivery.order.model.OrderStatus status = com.kuru.delivery.order.model.OrderStatus.valueOf(statusStr);
            OrderResponse response = orderService.updateOrderStatus(orderNumber, status);
            
            // Broadcast status update
            messagingTemplate.convertAndSend("/topic/orders/" + orderNumber, response);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
    
    private void validateDriverStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        switch (newStatus) {
            case PICKED_UP:
                if (currentStatus != OrderStatus.ASSIGNED) {
                    throw new IllegalStateException("Can only pick up orders with ASSIGNED status");
                }
                break;
            case ON_THE_WAY:
                if (currentStatus != OrderStatus.PICKED_UP && currentStatus != OrderStatus.ASSIGNED) {
                    throw new IllegalStateException("Invalid status transition to ON_THE_WAY");
                }
                break;
            case DELIVERED:
                if (currentStatus != OrderStatus.ON_THE_WAY && currentStatus != OrderStatus.PICKED_UP) {
                    throw new IllegalStateException("Can only deliver orders that are ON_THE_WAY or PICKED_UP");
                }
                break;
            default:
                throw new IllegalStateException("Drivers cannot set status to: " + newStatus);
        }
    }
    
    @PostMapping("/{id}/start-journey")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> startJourney(@PathVariable Long id) {
        try {
            Long driverUserId = getCurrentUserId();
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found"));
            
            if (order.getDriver() == null || order.getDriver().getUser() == null ||
                !order.getDriver().getUser().getId().equals(driverUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only start journey for orders assigned to you"));
            }
            
            order.setStatus(OrderStatus.ON_THE_WAY);
            orderRepository.save(order);
            messagingTemplate.convertAndSend("/topic/orders/" + order.getOrderNumber(), order);
            
            return ResponseEntity.ok("Journey started");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> completeOrder(@PathVariable Long id) {
        try {
            Long driverUserId = getCurrentUserId();
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found"));
            
            if (order.getDriver() == null || order.getDriver().getUser() == null ||
                !order.getDriver().getUser().getId().equals(driverUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only complete orders assigned to you"));
            }
            
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
            messagingTemplate.convertAndSend("/topic/orders/" + order.getOrderNumber(), order);
            
            return ResponseEntity.ok("Order completed");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @Autowired
    private RedisLocationService redisLocationService;
    
    @Autowired
    private com.kuru.delivery.order.repository.OrderRepository orderRepository;

    @GetMapping("/{orderId}/live-location")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getLiveLocation(@PathVariable Long orderId) {
        Long customerId = getCurrentUserId();
        com.kuru.delivery.order.model.Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        
        if (!order.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only track your own orders");
        }
        
        if (order.getDriver() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No driver assigned to this order");
        }
        
        Point point = redisLocationService.getDriverLocation(String.valueOf(order.getDriver().getId()));
        
        // Fallback to pickup location if driver location not available yet
        double lat = point != null ? point.getY() : order.getPickupLatitude();
        double lng = point != null ? point.getX() : order.getPickupLongitude();
        boolean isLive = point != null;
        
        return ResponseEntity.ok(Map.of(
            "latitude", lat,
            "longitude", lng,
            "isLive", isLive,
            "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/active-order")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> getActiveOrder(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Driver profile not found"));

        List<Order> activeOrders = orderRepository.findByDriverAndStatusIn(driver, 
            List.of(OrderStatus.ASSIGNED, OrderStatus.PICKED_UP, OrderStatus.ON_THE_WAY));
            
        if (activeOrders.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        Order order = activeOrders.get(0);
        OrderResponse response = orderService.mapToResponse(order);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            String email = userDetails.getUsername();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Driver driver = driverRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Driver profile not found"));

            List<Order> driverOrders = orderRepository.findByDriver(driver);
            List<OrderResponse> responses = driverOrders.stream()
                    .map(orderService::mapToResponse)
                    .toList();
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch driver orders", "message", e.getMessage()));
        }
    }

    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<?> trackOrder(@PathVariable String orderNumber) {
        try {
            OrderResponse order = orderService.getOrderByOrderNumber(orderNumber);
            return ResponseEntity.ok(order);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found"));
        }
    }

    @GetMapping("/track/{orderNumber}/eta")
    public ResponseEntity<?> getPublicETA(@PathVariable String orderNumber) {
        try {
            com.kuru.delivery.order.dto.ETAResponse eta = orderService.calculateETA(orderNumber);
            return ResponseEntity.ok(eta);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found"));
        }
    }

    @GetMapping("/track/{orderNumber}/live-location")
    public ResponseEntity<?> getPublicLiveLocation(@PathVariable String orderNumber) {
        com.kuru.delivery.order.model.Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        
        if (order.getDriver() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No driver assigned");
        }
        
        Point point = redisLocationService.getDriverLocation(String.valueOf(order.getDriver().getId()));

        // Fallback to pickup location if driver location not available yet
        double lat = point != null ? point.getY() : order.getPickupLatitude();
        double lng = point != null ? point.getX() : order.getPickupLongitude();
        boolean isLive = point != null;
        
        return ResponseEntity.ok(Map.of(
            "latitude", lat,
            "longitude", lng,
            "isLive", isLive,
            "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/{orderNumber}/eta")
    public ResponseEntity<?> getETA(@PathVariable String orderNumber) {
        try {
            com.kuru.delivery.order.dto.ETAResponse eta = orderService.calculateETA(orderNumber);
            return ResponseEntity.ok(eta);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found"));
        }
    }

    @PostMapping("/{orderNumber}/reorder")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> reorder(@PathVariable String orderNumber) {
        try {
            Long customerId = getCurrentUserId();
            com.kuru.delivery.order.dto.ReorderResponse response = orderService.reorder(orderNumber, customerId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (com.kuru.delivery.common.exception.ResourceAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email))
                .getId();
    }
}
