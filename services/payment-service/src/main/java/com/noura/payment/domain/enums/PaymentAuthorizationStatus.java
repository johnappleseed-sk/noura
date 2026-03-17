package com.noura.payment.domain.enums;

/**
 * Provider authorization sub-state for a payment transaction.
 */
public enum PaymentAuthorizationStatus {
    /**
     * No authorization attempt has been made yet.
     */
    NOT_REQUESTED,
    /**
     * Authorization is pending provider completion.
     */
    PENDING,
    /**
     * Authorization succeeded.
     */
    AUTHORIZED,
    /**
     * Authorization failed.
     */
    FAILED,
    /**
     * Authorization was canceled or voided.
     */
    CANCELED
}
