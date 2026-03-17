package com.noura.payment.domain.enums;

/**
 * Provider capture sub-state for a payment transaction.
 */
public enum PaymentCaptureStatus {
    /**
     * Capture has not been requested.
     */
    NOT_CAPTURED,
    /**
     * Capture is pending provider completion.
     */
    PENDING,
    /**
     * Capture succeeded.
     */
    CAPTURED,
    /**
     * Capture failed.
     */
    FAILED,
    /**
     * Authorization/capture path was canceled before settlement.
     */
    CANCELED,
    /**
     * Captured funds were refunded.
     */
    REFUNDED
}
