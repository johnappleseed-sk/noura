package com.noura.promotion.dto.promotion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request for validating a promo or coupon code.
 *
 * @param promoCode promo or coupon code
 * @param subtotal optional subtotal override
 * @param customerSegment optional customer segment
 * @param items optional evaluation items
 */
public record PromotionValidationRequest(
        @NotBlank(message = "promoCode is required")
        String promoCode,
        @DecimalMin(value = "0.00", message = "subtotal must be greater than or equal to 0")
        BigDecimal subtotal,
        String customerSegment,
        List<@Valid PromotionEvaluationItemRequest> items
) {
}
