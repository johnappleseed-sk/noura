package com.noura.order.domain.enums;

/**
 * Order lifecycle statuses used by order state machine validation.
 */
public enum OrderStatus {
    /**
     * Initial created status before review/payment pipeline.
     */
    CREATED,
    /**
     * Review step for special/manual approval flows.
     */
    REVIEWED,
    /**
     * Waiting for payment confirmation.
     */
    PAYMENT_PENDING,
    /**
     * Payment captured or confirmed.
     */
    PAID,
    /**
     * Internal processing started.
     */
    PROCESSING,
    /**
     * Items packed and awaiting shipment handoff.
     */
    PACKED,
    /**
     * Shipment left fulfillment location.
     */
    SHIPPED,
    /**
     * Delivery completed.
     */
    DELIVERED,
    /**
     * Order cancelled.
     */
    CANCELLED,
    /**
     * Order refunded.
     */
    REFUNDED
}

