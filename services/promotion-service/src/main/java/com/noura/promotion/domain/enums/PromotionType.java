package com.noura.promotion.domain.enums;

/**
 * Supported promotion rule types.
 */
public enum PromotionType {
    /**
     * Percentage discount against the eligible subtotal.
     */
    PERCENTAGE,
    /**
     * Fixed amount discount against the eligible subtotal.
     */
    FIXED,
    /**
     * Buy X quantity and get Y discounted quantity.
     */
    BUY_X_GET_Y,
    /**
     * Shipping is discounted to zero when eligible.
     */
    FREE_SHIPPING,
    /**
     * Threshold-gated cart-level discount using percent or fixed amount.
     */
    CART_THRESHOLD_DISCOUNT,
    /**
     * Bundle or set discount when required products are present.
     */
    PRODUCT_BUNDLE_DISCOUNT
}
