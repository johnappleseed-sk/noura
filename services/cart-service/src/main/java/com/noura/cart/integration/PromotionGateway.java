package com.noura.cart.integration;

import com.noura.cart.integration.model.PromotionEvaluationItem;
import com.noura.cart.integration.model.PromotionValidationSnapshot;

import java.math.BigDecimal;
import java.util.List;

/**
 * Downstream promotion integration port for coupon validation and discount evaluation.
 */
public interface PromotionGateway {

    /**
     * Validates one coupon/promo code against the current cart snapshot.
     *
     * @param couponCode coupon code value
     * @param subtotal cart subtotal
     * @param items cart line snapshots used for eligibility checks
     * @param correlationId request correlation ID
     * @return promotion validation snapshot
     */
    PromotionValidationSnapshot validateCoupon(
            String couponCode,
            BigDecimal subtotal,
            List<PromotionEvaluationItem> items,
            String correlationId
    );
}
