package com.noura.promotion.dto.promotion;

/**
 * Result of validating one promo or coupon code.
 *
 * @param valid whether the promo code exists and is currently addressable
 * @param eligible whether the current cart/input qualifies
 * @param reasonCode stable machine-readable reason code
 * @param reasonMessage human-readable reason message
 * @param promotion matched promotion, when present
 * @param evaluation evaluation result, when eligible or partially evaluable
 */
public record PromotionValidationResponse(
        boolean valid,
        boolean eligible,
        String reasonCode,
        String reasonMessage,
        PromotionResponse promotion,
        PromotionEvaluationResponse evaluation
) {
}
