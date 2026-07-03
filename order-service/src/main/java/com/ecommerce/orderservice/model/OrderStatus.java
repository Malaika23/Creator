package com.ecommerce.orderservice.model;

public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
