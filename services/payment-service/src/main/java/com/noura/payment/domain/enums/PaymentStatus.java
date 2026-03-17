package com.noura.payment.domain.enums;

/**
 * Lifecycle states for payment transactions.
 */
public enum PaymentStatus {
    /**
     * Internal payment record has been created before provider confirmation.
     */
    CREATED,
    /**
     * Provider intent exists and awaits an authorize/capture confirmation step.
     */
    REQUIRES_CONFIRMATION,
    /**
     * Provider is still processing an asynchronous authorization or capture result.
     */
    PENDING,
    /**
     * Payment was authorized but not yet captured.
     */
    AUTHORIZED,
    /**
     * Payment was captured successfully.
     */
    CAPTURED,
    /**
     * Payment was refunded after capture.
     */
    REFUNDED,
    /**
     * Payment failed.
     */
    FAILED,
    /**
     * Payment was canceled.
     */
    CANCELED
}
