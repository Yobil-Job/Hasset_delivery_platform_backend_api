package com.kuru.delivery.user.service;

import com.kuru.delivery.order.model.Order;
import com.kuru.delivery.order.repository.OrderRepository;
import com.kuru.delivery.user.dto.AnalyticsResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final OrderRepository orderRepository;

    public AnalyticsService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public AnalyticsResponse getCustomerAnalytics(Long customerId, String period) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        
        // Filter orders by period
        LocalDateTime startDate = getStartDateForPeriod(period);
        List<Order> filteredOrders = orders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());

        if (filteredOrders.isEmpty()) {
            return createEmptyAnalytics();
        }

        // Calculate basic statistics
        Long totalOrders = (long) filteredOrders.size();
        BigDecimal totalSpent = filteredOrders.stream()
                .map(Order::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageOrderValue = totalSpent.divide(
                BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

        // Find favorite service
        AnalyticsResponse.FavoriteService favoriteService = findFavoriteService(filteredOrders);

        // Calculate spending trend
        List<AnalyticsResponse.SpendingTrend> spendingTrend = calculateSpendingTrend(filteredOrders, period);

        // Calculate order frequency
        String orderFrequency = calculateOrderFrequency(filteredOrders);

        // Top locations
        List<AnalyticsResponse.LocationStats> topPickupLocations = getTopLocations(
                filteredOrders, true, 5);
        List<AnalyticsResponse.LocationStats> topDeliveryLocations = getTopLocations(
                filteredOrders, false, 5);

        return new AnalyticsResponse(
                totalOrders,
                totalSpent,
                averageOrderValue,
                favoriteService,
                spendingTrend,
                orderFrequency,
                topPickupLocations,
                topDeliveryLocations
        );
    }

    private LocalDateTime getStartDateForPeriod(String period) {
        LocalDate now = LocalDate.now();
        switch (period.toLowerCase()) {
            case "week":
                return now.minusWeeks(1).atStartOfDay();
            case "month":
                return now.minusMonths(1).atStartOfDay();
            case "year":
                return now.minusYears(1).atStartOfDay();
            default:
                return now.minusMonths(1).atStartOfDay(); // Default to month
        }
    }

    private AnalyticsResponse.FavoriteService findFavoriteService(List<Order> orders) {
        Map<Long, Long> serviceCounts = orders.stream()
                .filter(order -> order.getServiceOffering() != null)
                .collect(Collectors.groupingBy(
                        order -> order.getServiceOffering().getId(),
                        Collectors.counting()
                ));

        if (serviceCounts.isEmpty()) {
            return null;
        }

        Map.Entry<Long, Long> favorite = serviceCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (favorite == null) {
            return null;
        }

        Order orderWithService = orders.stream()
                .filter(order -> order.getServiceOffering() != null &&
                        order.getServiceOffering().getId().equals(favorite.getKey()))
                .findFirst()
                .orElse(null);

        if (orderWithService == null || orderWithService.getServiceOffering() == null) {
            return null;
        }

        return new AnalyticsResponse.FavoriteService(
                favorite.getKey(),
                orderWithService.getServiceOffering().getTitle(),
                favorite.getValue()
        );
    }

    private List<AnalyticsResponse.SpendingTrend> calculateSpendingTrend(
            List<Order> orders, String period) {
        Map<String, BigDecimal> trendMap = new TreeMap<>();

        DateTimeFormatter formatter;
        switch (period.toLowerCase()) {
            case "week":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                orders.forEach(order -> {
                    String day = order.getCreatedAt().format(formatter);
                    trendMap.merge(day, order.getPrice(), BigDecimal::add);
                });
                break;
            case "month":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                orders.forEach(order -> {
                    String month = order.getCreatedAt().format(formatter);
                    trendMap.merge(month, order.getPrice(), BigDecimal::add);
                });
                break;
            case "year":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                orders.forEach(order -> {
                    String month = order.getCreatedAt().format(formatter);
                    trendMap.merge(month, order.getPrice(), BigDecimal::add);
                });
                break;
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                orders.forEach(order -> {
                    String month = order.getCreatedAt().format(formatter);
                    trendMap.merge(month, order.getPrice(), BigDecimal::add);
                });
        }

        return trendMap.entrySet().stream()
                .map(entry -> new AnalyticsResponse.SpendingTrend(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private String calculateOrderFrequency(List<Order> orders) {
        if (orders.size() < 2) {
            return "N/A";
        }

        // Calculate average days between orders
        List<LocalDateTime> sortedDates = orders.stream()
                .map(Order::getCreatedAt)
                .sorted()
                .collect(Collectors.toList());

        long totalDays = 0;
        for (int i = 1; i < sortedDates.size(); i++) {
            totalDays += java.time.Duration.between(
                    sortedDates.get(i - 1), sortedDates.get(i)).toDays();
        }

        double avgDays = (double) totalDays / (sortedDates.size() - 1);

        if (avgDays < 1) {
            return "daily";
        } else if (avgDays < 7) {
            return "weekly";
        } else if (avgDays < 30) {
            return "monthly";
        } else {
            return "occasionally";
        }
    }

    private List<AnalyticsResponse.LocationStats> getTopLocations(
            List<Order> orders, boolean isPickup, int limit) {
        Map<String, Long> locationCounts = orders.stream()
                .map(order -> isPickup ? order.getPickupAddress() : order.getDeliveryAddress())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        address -> address,
                        Collectors.counting()
                ));

        return locationCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new AnalyticsResponse.LocationStats(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private AnalyticsResponse createEmptyAnalytics() {
        return new AnalyticsResponse(
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                Collections.emptyList(),
                "N/A",
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}

