package com.noura.platform.commerce.b2b.domain;

/**
 * Status of a B2B Purchase Order.
 */
public enum POStatus {
    /** Order is being prepared */
    DRAFT,

    /** Submitted for approval */
    PENDING_APPROVAL,

    /** Approved, ready for fulfillment */
    APPROVED,

    /** Order rejected */
    REJECTED,

    /** Being processed/picked */
    IN_PROGRESS,

    /** Partially shipped */
    PARTIALLY_SHIPPED,

    /** Fully shipped */
    SHIPPED,

    /** Order cancelled */
    CANCELLED,

    /** Order closed (completed) */
    CLOSED
}
