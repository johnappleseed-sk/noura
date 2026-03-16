package com.noura.cart.integration.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Normalized pricing snapshot fetched from pricing service.
 *
 * @param productId product identifier
 * @param unitPrice resolved effective unit price
 * @param currencyCode resolved currency code
 */
public record PricingSnapshot(
        UUID productId,
        BigDecimal unitPrice,
        String currencyCode
) {
}
