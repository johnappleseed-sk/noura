package com.noura.cart.integration.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cart-line snapshot used for promotion-service coupon evaluation requests.
 *
 * @param productId product identifier
 * @param variantId optional variant identifier
 * @param quantity item quantity
 * @param unitPrice item unit price
 */
public record PromotionEvaluationItem(
        UUID productId,
        UUID variantId,
        int quantity,
        BigDecimal unitPrice
) {
}
