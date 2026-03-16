package com.noura.platform.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResult(
        Long id,
        String provider,
        String paymentMethod,
        String status,
        BigDecimal amount,
        String currencyCode,
        String providerReference,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
