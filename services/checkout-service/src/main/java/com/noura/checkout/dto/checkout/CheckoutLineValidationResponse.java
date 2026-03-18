package com.noura.checkout.dto.checkout;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Per-line checkout validation result.
 *
 * @param lineItemId cart line identifier
 * @param productId product identifier
 * @param variantId optional variant identifier
 * @param productName product display name snapshot
 * @param sku sku snapshot
 * @param quantity requested quantity
 * @param unitPrice resolved unit price
 * @param lineTotal resolved line total
 * @param availableQuantity resolved available stock
 * @param storeId resolved location identifier
 * @param valid indicates whether this line is valid for checkout
 * @param issueCode optional machine-readable issue code
 * @param issueMessage optional human-readable issue message
 */
public record CheckoutLineValidationResponse(
        UUID lineItemId,
        UUID productId,
        UUID variantId,
        String productName,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        BigDecimal availableQuantity,
        UUID storeId,
        boolean valid,
        String issueCode,
        String issueMessage
) {
}

