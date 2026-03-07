package com.noura.platform.commerce.orders.domain;

public enum OrderStatus {
    DRAFT,
    PENDING_PAYMENT,
    CONFIRMED,
    PAID,
    PROCESSING,
    FULFILLED,
    CANCELLED,
    PAYMENT_FAILED,
    REFUNDED
}
