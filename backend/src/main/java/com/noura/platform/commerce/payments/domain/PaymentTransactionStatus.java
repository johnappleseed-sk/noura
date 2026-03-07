package com.noura.platform.commerce.payments.domain;

public enum PaymentTransactionStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    VOIDED
}
