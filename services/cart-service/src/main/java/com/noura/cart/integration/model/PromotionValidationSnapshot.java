package com.noura.cart.integration.model;

/**
 * Coupon/promo validation snapshot returned by promotion integration.
 *
 * @param valid whether promo code exists
 * @param eligible whether cart snapshot qualifies
 * @param reasonCode machine-readable reason code
 * @param reasonMessage human-readable reason message
 * @param evaluation discount evaluation payload
 */
public record PromotionValidationSnapshot(
        boolean valid,
        boolean eligible,
        String reasonCode,
        String reasonMessage,
        PromotionEvaluationSnapshot evaluation
) {
}
