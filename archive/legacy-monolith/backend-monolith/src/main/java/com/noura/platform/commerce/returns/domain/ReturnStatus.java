package com.noura.platform.commerce.returns.domain;

/**
 * Status of a return request through its lifecycle.
 */
public enum ReturnStatus {
    /** Initial state when customer submits return request */
    PENDING_REVIEW,

    /** Return has been approved, awaiting item receipt */
    APPROVED,

    /** Return was rejected by staff */
    REJECTED,

    /** Items have been received at warehouse */
    ITEMS_RECEIVED,

    /** Return is being inspected/processed */
    PROCESSING,

    /** Refund has been issued */
    REFUNDED,

    /** Return completed successfully */
    COMPLETED,

    /** Return was cancelled by customer or staff */
    CANCELLED
}
