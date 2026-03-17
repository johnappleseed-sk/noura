package com.noura.promotion.dto.promotion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Result of cart promotion evaluation.
 *
 * @param subtotal evaluated subtotal
 * @param discountAmount total discount amount
 * @param discountedSubtotal subtotal after discount
 * @param freeShipping whether shipping should be discounted to zero
 * @param appliedPromotionIds applied promotion identifiers
 * @param appliedPromotionCodes applied promotion codes or names
 */
public record PromotionEvaluationResponse(
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal discountedSubtotal,
        boolean freeShipping,
        List<UUID> appliedPromotionIds,
        List<String> appliedPromotionCodes
) {
}
