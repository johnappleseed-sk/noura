package com.noura.payment.domain.enums;

/**
 * Processing state for one inbound webhook delivery.
 */
public enum PaymentWebhookProcessingStatus {
    /**
     * Delivery has been persisted but not fully processed yet.
     */
    RECEIVED,
    /**
     * Delivery successfully updated payment state.
     */
    PROCESSED,
    /**
     * Delivery was accepted but did not change payment state.
     */
    IGNORED,
    /**
     * Delivery could not be applied.
     */
    FAILED
}
