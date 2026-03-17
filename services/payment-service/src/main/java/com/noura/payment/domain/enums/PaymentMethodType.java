package com.noura.payment.domain.enums;

/**
 * Payment method classification used by payment intents.
 */
public enum PaymentMethodType {
    /**
     * Card-based payment method.
     */
    CARD,
    /**
     * Wallet-based payment method.
     */
    WALLET,
    /**
     * Bank transfer payment method.
     */
    BANK_TRANSFER,
    /**
     * Cash on delivery placeholder method.
     */
    CASH_ON_DELIVERY
}
