package com.noura.checkout.domain.enums;

/**
 * Payment methods accepted by checkout-service request contracts.
 */
public enum CheckoutPaymentMethod {
    /**
     * Card-based checkout payment.
     */
    CREDIT_CARD,
    /**
     * Cash-on-delivery placeholder payment.
     */
    CASH_ON_DELIVERY,
    /**
     * Bank transfer payment.
     */
    BANK_TRANSFER,
    /**
     * Wallet payment.
     */
    WALLET
}
