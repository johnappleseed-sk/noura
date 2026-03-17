package com.noura.payment.domain.enums;

/**
 * Confirmation command actions supported by payment provider adapters.
 */
public enum PaymentConfirmationAction {
    /**
     * Authorize funds only.
     */
    AUTHORIZE,
    /**
     * Capture funds.
     */
    CAPTURE
}
