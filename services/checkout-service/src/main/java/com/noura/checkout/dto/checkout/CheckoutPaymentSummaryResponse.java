package com.noura.checkout.dto.checkout;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment summary returned after synchronous checkout payment orchestration.
 *
 * @param paymentId payment identifier
 * @param paymentReference internal payment reference
 * @param providerCode resolved provider code
 * @param methodType resolved payment method type
 * @param status payment lifecycle status
 * @param amount payment amount
 * @param currencyCode payment currency code
 * @param confirmedAt payment confirmation timestamp
 */
public record CheckoutPaymentSummaryResponse(
        UUID paymentId,
        String paymentReference,
        String providerCode,
        String methodType,
        String status,
        BigDecimal amount,
        String currencyCode,
        Instant confirmedAt
) {
}
