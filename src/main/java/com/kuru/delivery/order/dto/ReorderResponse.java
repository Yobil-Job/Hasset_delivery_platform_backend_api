package com.kuru.delivery.order.dto;

import java.math.BigDecimal;

public class ReorderResponse {
    private String newOrderNumber;
    private Long orderId;
    private BigDecimal price;

    public ReorderResponse() {}

    public ReorderResponse(String newOrderNumber, Long orderId, BigDecimal price) {
        this.newOrderNumber = newOrderNumber;
        this.orderId = orderId;
        this.price = price;
    }

    public String getNewOrderNumber() {
        return newOrderNumber;
    }

    public void setNewOrderNumber(String newOrderNumber) {
        this.newOrderNumber = newOrderNumber;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}

