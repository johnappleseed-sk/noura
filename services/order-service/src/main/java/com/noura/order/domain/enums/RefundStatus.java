package com.noura.order.domain.enums;

/**
 * Refund processing status tracked on each order.
 */
public enum RefundStatus {
    /**
     * No refund workflow exists for the order.
     */
    NONE,
    /**
     * Refund requested but not yet reviewed.
     */
    REQUESTED,
    /**
     * Refund request approved.
     */
    APPROVED,
    /**
     * Refund request rejected.
     */
    REJECTED,
    /**
     * Refund completed.
     */
    COMPLETED
}

