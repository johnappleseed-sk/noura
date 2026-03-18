package com.noura.cart.integration.model;

import java.math.BigDecimal;

/**
 * Discount-evaluation snapshot returned by promotion integration.
 *
 * @param discountAmount discount amount
 * @param freeShipping whether free shipping applies
 */
public record PromotionEvaluationSnapshot(
        BigDecimal discountAmount,
        boolean freeShipping
) {
}
