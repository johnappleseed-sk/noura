package com.noura.platform.dto.payment;

public record CreatePaymentRequest(
        String paymentMethod,
        String provider,
        String providerReference
) {
}
