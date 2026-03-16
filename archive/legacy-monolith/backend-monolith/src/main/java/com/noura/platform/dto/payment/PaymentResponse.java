package com.noura.platform.dto.payment;

import com.noura.platform.domain.enums.PaymentMethodType;
import com.noura.platform.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String paymentReference,
        PaymentMethodType methodType,
        PaymentStatus status,
        String providerCode,
        String providerTransactionId,
        BigDecimal amount,
        String currencyCode,
        Instant requestedAt,
        Instant completedAt,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
