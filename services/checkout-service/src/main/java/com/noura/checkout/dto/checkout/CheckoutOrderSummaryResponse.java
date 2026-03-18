package com.noura.checkout.dto.checkout;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order summary returned after successful checkout orchestration.
 *
 * @param orderId order identifier
 * @param orderNumber business order number
 * @param status order status
 * @param totalAmount order total amount
 * @param currencyCode order currency code
 * @param placedAt order placement timestamp
 */
public record CheckoutOrderSummaryResponse(
        UUID orderId,
        String orderNumber,
        String status,
        BigDecimal totalAmount,
        String currencyCode,
        Instant placedAt
) {
}

