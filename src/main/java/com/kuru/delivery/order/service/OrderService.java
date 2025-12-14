package com.kuru.delivery.order.service;

import com.kuru.delivery.location.service.RedisLocationService;
import com.kuru.delivery.order.dto.CreateOrderRequest;
import com.kuru.delivery.order.dto.ETAResponse;
import com.kuru.delivery.order.dto.OrderResponse;
import com.kuru.delivery.order.dto.ReorderResponse;
import com.kuru.delivery.order.model.Order;
import com.kuru.delivery.order.model.OrderStatus;
import com.kuru.delivery.order.repository.OrderRepository;
import com.kuru.delivery.pricing.model.ServiceOffering;
import com.kuru.delivery.pricing.repository.ServiceOfferingRepository;
import com.kuru.delivery.pricing.service.PricingService;
import com.kuru.delivery.user.service.RecentAddressService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final PricingService pricingService;
    private final RedisLocationService redisLocationService;
    private final RecentAddressService recentAddressService;

    // Average speeds in km/h
    private static final double CITY_SPEED = 30.0;
    private static final double HIGHWAY_SPEED = 60.0;
    private static final double DEFAULT_SPEED = 35.0; // Default average speed

    public OrderService(OrderRepository orderRepository,
                       ServiceOfferingRepository serviceOfferingRepository,
                       PricingService pricingService,
                       RedisLocationService redisLocationService,
                       RecentAddressService recentAddressService) {
        this.orderRepository = orderRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.pricingService = pricingService;
        this.redisLocationService = redisLocationService;
        this.recentAddressService = recentAddressService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long customerId) {
        validateOrderRequest(request);
        
        ServiceOffering serviceOffering = serviceOfferingRepository.findById(request.getServiceId())
                .orElseThrow(() -> new EntityNotFoundException("Service offering not found"));

        // CRITICAL: Always calculate distance on backend - never trust frontend
        double distanceKm = calculateDistance(
            request.getPickupLatitude(), request.getPickupLongitude(),
            request.getDeliveryLatitude(), request.getDeliveryLongitude()
        );
        
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Invalid distance calculated. Please check pickup and delivery locations.");
        }
        if (distanceKm > 10000) {
            throw new IllegalArgumentException("Distance exceeds maximum allowed limit.");
        }

        // CRITICAL: Always calculate price on backend - never trust frontend
        BigDecimal price = pricingService.calculateTotalPrice(
                request.getWeightKg(),
                distanceKm,
                request.getServiceId()
        );
        
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Calculated price must be positive");
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerId(customerId);
        order.setServiceOffering(serviceOffering);
        order.setWeightKg(request.getWeightKg());
        order.setDistanceKm(distanceKm);
        order.setPrice(price);
        order.setPickupAddress(request.getPickupAddress());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setPickupLatitude(request.getPickupLatitude());
        order.setPickupLongitude(request.getPickupLongitude());
        order.setDeliveryLatitude(request.getDeliveryLatitude());
        order.setDeliveryLongitude(request.getDeliveryLongitude());
        order.setStatus(OrderStatus.CREATED);

        Order savedOrder = orderRepository.save(order);
        
        // Track addresses as recent addresses
        try {
            recentAddressService.saveOrUpdateAddress(
                    customerId, 
                    request.getPickupAddress(), 
                    request.getPickupLatitude(), 
                    request.getPickupLongitude(), 
                    "pickup"
            );
            recentAddressService.saveOrUpdateAddress(
                    customerId, 
                    request.getDeliveryAddress(), 
                    request.getDeliveryLatitude(), 
                    request.getDeliveryLongitude(), 
                    "delivery"
            );
        } catch (Exception e) {
            // Log but don't fail order creation if address tracking fails
            logger.warn("Failed to track recent addresses: {}", e.getMessage());
        }
        
        return mapToResponse(savedOrder);
    }

    /**
     * Server-side validation of order request (defense in depth).
     * Validates all inputs to prevent data injection and manipulation.
     */
    private void validateOrderRequest(CreateOrderRequest request) {
        // Weight validation
        if (request.getWeightKg() == null || request.getWeightKg() <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
        if (request.getWeightKg() > 1000) {
            throw new IllegalArgumentException("Weight cannot exceed 1000 kg");
        }
        
        // Coordinate validation
        if (request.getPickupLatitude() == null || request.getPickupLongitude() == null ||
            request.getDeliveryLatitude() == null || request.getDeliveryLongitude() == null) {
            throw new IllegalArgumentException("All coordinates are required");
        }
        
        // Validate latitude range
        if (request.getPickupLatitude() < -90 || request.getPickupLatitude() > 90 ||
            request.getDeliveryLatitude() < -90 || request.getDeliveryLatitude() > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        
        // Validate longitude range
        if (request.getPickupLongitude() < -180 || request.getPickupLongitude() > 180 ||
            request.getDeliveryLongitude() < -180 || request.getDeliveryLongitude() > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        
        // Service ID validation
        if (request.getServiceId() == null || request.getServiceId() <= 0) {
            throw new IllegalArgumentException("Service ID must be positive");
        }
        
        // Address validation
        if (request.getPickupAddress() == null || request.getPickupAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Pickup address is required");
        }
        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery address is required");
        }
        if (request.getPickupAddress().length() > 500 || request.getDeliveryAddress().length() > 500) {
            throw new IllegalArgumentException("Address cannot exceed 500 characters");
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        return mapToResponse(order);
    }

    /**
     * Gets order by order number and validates ownership.
     * Throws ResourceAccessDeniedException if user doesn't own the order.
     */
    public OrderResponse getOrderByOrderNumberForUser(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        
        // Validate ownership
        if (!order.getCustomerId().equals(userId)) {
            throw new com.kuru.delivery.common.exception.ResourceAccessDeniedException(
                "You do not have permission to access this order"
            );
        }
        
        return mapToResponse(order);
    }

    public List<OrderResponse> getOrdersForCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersForCustomerWithFilters(
            Long customerId,
            String status,
            String startDate,
            String endDate,
            Double minPrice,
            Double maxPrice,
            Long serviceId) {
        
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        
        return orders.stream()
                .filter(order -> {
                    // Filter by status
                    if (status != null && !status.isEmpty()) {
                        try {
                            OrderStatus filterStatus = OrderStatus.valueOf(status.toUpperCase());
                            if (order.getStatus() != filterStatus) {
                                return false;
                            }
                        } catch (IllegalArgumentException e) {
                            // Invalid status, skip filter
                        }
                    }
                    
                    // Filter by date range
                    if (startDate != null && !startDate.isEmpty()) {
                        try {
                            LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
                            if (order.getCreatedAt().toLocalDate().isBefore(start)) {
                                return false;
                            }
                        } catch (Exception e) {
                            // Invalid date, skip filter
                        }
                    }
                    
                    if (endDate != null && !endDate.isEmpty()) {
                        try {
                            LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
                            if (order.getCreatedAt().toLocalDate().isAfter(end)) {
                                return false;
                            }
                        } catch (Exception e) {
                            // Invalid date, skip filter
                        }
                    }
                    
                    // Filter by price range
                    if (minPrice != null) {
                        if (order.getPrice().doubleValue() < minPrice) {
                            return false;
                        }
                    }
                    
                    if (maxPrice != null) {
                        if (order.getPrice().doubleValue() > maxPrice) {
                            return false;
                        }
                    }
                    
                    // Filter by service ID
                    if (serviceId != null) {
                        if (order.getServiceOffering() == null || 
                            !order.getServiceOffering().getId().equals(serviceId)) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse cancelOrder(String orderNumber, Long customerId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        // Validate ownership
        if (!order.getCustomerId().equals(customerId)) {
            throw new com.kuru.delivery.common.exception.ResourceAccessDeniedException(
                "You do not have permission to cancel this order"
            );
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Only orders with status CREATED can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);
        return mapToResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderNumber, OrderStatus status) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        return mapToResponse(updatedOrder);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public ETAResponse calculateETA(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        ETAResponse etaResponse = new ETAResponse();
        etaResponse.setCurrentStatus(order.getStatus());

        // If order is not in transit, return default response
        if (order.getStatus() != OrderStatus.ON_THE_WAY && 
            order.getStatus() != OrderStatus.PICKED_UP) {
            etaResponse.setEstimatedMinutes(0);
            etaResponse.setEstimatedArrival(null);
            etaResponse.setRemainingDistanceKm(0);
            etaResponse.setAverageSpeedKmh(0);
            etaResponse.setDestinationLocation(new ETAResponse.Location(
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude()
            ));
            return etaResponse;
        }

        // Get driver location from Redis
        Point driverPoint = null;
        if (order.getDriver() != null) {
            driverPoint = redisLocationService.getDriverLocation(String.valueOf(order.getDriver().getId()));
        }

        // Use driver location if available, otherwise use pickup location
        double currentLat = driverPoint != null ? driverPoint.getY() : order.getPickupLatitude();
        double currentLng = driverPoint != null ? driverPoint.getX() : order.getPickupLongitude();

        // Calculate remaining distance
        double remainingDistance = calculateDistance(
            currentLat, currentLng,
            order.getDeliveryLatitude(), order.getDeliveryLongitude()
        );

        // Determine average speed based on distance and location type
        // For city deliveries (shorter distances), use city speed
        // For longer distances, use highway speed
        double averageSpeed = remainingDistance < 10 ? CITY_SPEED : 
                             remainingDistance < 30 ? DEFAULT_SPEED : HIGHWAY_SPEED;

        // Calculate estimated time in minutes
        double estimatedHours = remainingDistance / averageSpeed;
        int estimatedMinutes = (int) Math.ceil(estimatedHours * 60);

        // Calculate estimated arrival time
        LocalDateTime estimatedArrival = LocalDateTime.now().plusMinutes(estimatedMinutes);

        // Set response
        etaResponse.setEstimatedArrivalFromLocalDateTime(estimatedArrival);
        etaResponse.setEstimatedMinutes(estimatedMinutes);
        etaResponse.setRemainingDistanceKm(remainingDistance);
        etaResponse.setAverageSpeedKmh(averageSpeed);
        etaResponse.setDriverLocation(new ETAResponse.Location(currentLat, currentLng));
        etaResponse.setDestinationLocation(new ETAResponse.Location(
            order.getDeliveryLatitude(),
            order.getDeliveryLongitude()
        ));

        return etaResponse;
    }

    @Transactional
    public ReorderResponse reorder(String orderNumber, Long customerId) {
        // Find the original order
        Order originalOrder = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        // Validate ownership
        if (!originalOrder.getCustomerId().equals(customerId)) {
            throw new com.kuru.delivery.common.exception.ResourceAccessDeniedException(
                "You do not have permission to reorder this order"
            );
        }

        // Verify the service still exists
        ServiceOffering serviceOffering = originalOrder.getServiceOffering();
        if (serviceOffering == null) {
            throw new IllegalStateException("Service offering no longer available");
        }

        // Recalculate distance (in case coordinates changed, though unlikely)
        double distanceKm = calculateDistance(
            originalOrder.getPickupLatitude(), originalOrder.getPickupLongitude(),
            originalOrder.getDeliveryLatitude(), originalOrder.getDeliveryLongitude()
        );

        // Recalculate price (prices may have changed)
        BigDecimal newPrice = pricingService.calculateTotalPrice(
            originalOrder.getWeightKg(),
            distanceKm,
            serviceOffering.getId()
        );

        // Create new order with same details
        Order newOrder = new Order();
        newOrder.setOrderNumber(generateOrderNumber());
        newOrder.setCustomerId(customerId);
        newOrder.setServiceOffering(serviceOffering);
        newOrder.setWeightKg(originalOrder.getWeightKg());
        newOrder.setDistanceKm(distanceKm);
        newOrder.setPrice(newPrice);
        newOrder.setPickupAddress(originalOrder.getPickupAddress());
        newOrder.setDeliveryAddress(originalOrder.getDeliveryAddress());
        newOrder.setPickupLatitude(originalOrder.getPickupLatitude());
        newOrder.setPickupLongitude(originalOrder.getPickupLongitude());
        newOrder.setDeliveryLatitude(originalOrder.getDeliveryLatitude());
        newOrder.setDeliveryLongitude(originalOrder.getDeliveryLongitude());
        newOrder.setStatus(OrderStatus.CREATED);

        Order savedOrder = orderRepository.save(newOrder);

        return new ReorderResponse(
            savedOrder.getOrderNumber(),
            savedOrder.getId(),
            savedOrder.getPrice()
        );
    }

    public OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setWeightKg(order.getWeightKg());
        response.setDistanceKm(order.getDistanceKm());
        response.setPrice(order.getPrice());
        response.setPickupAddress(order.getPickupAddress());
        response.setDeliveryAddress(order.getDeliveryAddress());
        response.setPickupLatitude(order.getPickupLatitude());
        response.setPickupLongitude(order.getPickupLongitude());
        response.setDeliveryLatitude(order.getDeliveryLatitude());
        response.setDeliveryLongitude(order.getDeliveryLongitude());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setDriver(order.getDriver());
        response.setServiceOffering(order.getServiceOffering());
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        return response;
    }
}
