package com.noura.promotion.dto.promotion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request for evaluating cart promotions and coupon discounts.
 *
 * @param subtotal optional subtotal override; computed from items when omitted
 * @param promoCode optional promo or coupon code
 * @param customerSegment optional exact-match customer segment
 * @param items optional evaluation items
 */
public record PromotionEvaluationRequest(
        @DecimalMin(value = "0.00", message = "subtotal must be greater than or equal to 0")
        BigDecimal subtotal,
        String promoCode,
        String customerSegment,
        List<@Valid PromotionEvaluationItemRequest> items
) {
}
