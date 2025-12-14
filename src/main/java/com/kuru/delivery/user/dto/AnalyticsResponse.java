package com.kuru.delivery.user.dto;

import java.math.BigDecimal;
import java.util.List;

public class AnalyticsResponse {
    private Long totalOrders;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
    private FavoriteService favoriteService;
    private List<SpendingTrend> spendingTrend;
    private String orderFrequency;
    private List<LocationStats> topPickupLocations;
    private List<LocationStats> topDeliveryLocations;

    public AnalyticsResponse() {}

    public AnalyticsResponse(Long totalOrders, BigDecimal totalSpent, BigDecimal averageOrderValue,
                           FavoriteService favoriteService, List<SpendingTrend> spendingTrend,
                           String orderFrequency, List<LocationStats> topPickupLocations,
                           List<LocationStats> topDeliveryLocations) {
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
        this.averageOrderValue = averageOrderValue;
        this.favoriteService = favoriteService;
        this.spendingTrend = spendingTrend;
        this.orderFrequency = orderFrequency;
        this.topPickupLocations = topPickupLocations;
        this.topDeliveryLocations = topDeliveryLocations;
    }

    // Getters and Setters
    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public FavoriteService getFavoriteService() {
        return favoriteService;
    }

    public void setFavoriteService(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    public List<SpendingTrend> getSpendingTrend() {
        return spendingTrend;
    }

    public void setSpendingTrend(List<SpendingTrend> spendingTrend) {
        this.spendingTrend = spendingTrend;
    }

    public String getOrderFrequency() {
        return orderFrequency;
    }

    public void setOrderFrequency(String orderFrequency) {
        this.orderFrequency = orderFrequency;
    }

    public List<LocationStats> getTopPickupLocations() {
        return topPickupLocations;
    }

    public void setTopPickupLocations(List<LocationStats> topPickupLocations) {
        this.topPickupLocations = topPickupLocations;
    }

    public List<LocationStats> getTopDeliveryLocations() {
        return topDeliveryLocations;
    }

    public void setTopDeliveryLocations(List<LocationStats> topDeliveryLocations) {
        this.topDeliveryLocations = topDeliveryLocations;
    }

    // Nested classes
    public static class FavoriteService {
        private Long id;
        private String name;
        private Long orderCount;

        public FavoriteService() {}

        public FavoriteService(Long id, String name, Long orderCount) {
            this.id = id;
            this.name = name;
            this.orderCount = orderCount;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(Long orderCount) {
            this.orderCount = orderCount;
        }
    }

    public static class SpendingTrend {
        private String period;
        private BigDecimal amount;

        public SpendingTrend() {}

        public SpendingTrend(String period, BigDecimal amount) {
            this.period = period;
            this.amount = amount;
        }

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class LocationStats {
        private String address;
        private Long count;

        public LocationStats() {}

        public LocationStats(String address, Long count) {
            this.address = address;
            this.count = count;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}

