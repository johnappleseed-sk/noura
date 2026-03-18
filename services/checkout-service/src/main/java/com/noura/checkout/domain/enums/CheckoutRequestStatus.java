package com.noura.checkout.domain.enums;

/**
 * State machine for checkout idempotency request records.
 */
public enum CheckoutRequestStatus {
    /**
     * Request was accepted and is currently being processed.
     */
    PROCESSING,
    /**
     * Request finished successfully and produced a stable response.
     */
    SUCCEEDED,
    /**
     * Request failed and can be retried with the same idempotency key.
     */
    FAILED
}

