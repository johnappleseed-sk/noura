package com.noura.cart.domain.enums;

/**
 * Validation snapshot for a cart line against downstream services.
 */
public enum CartItemValidationStatus {
    /**
     * Line is valid and can proceed to checkout.
     */
    VALID,
    /**
     * Product no longer exists in catalog.
     */
    PRODUCT_NOT_FOUND,
    /**
     * Pricing service could not resolve an effective price.
     */
    PRICE_UNAVAILABLE,
    /**
     * Product has no available quantity.
     */
    OUT_OF_STOCK,
    /**
     * Available quantity is below requested line quantity.
     */
    INSUFFICIENT_STOCK,
    /**
     * Validation could not complete due to temporary dependency error.
     */
    UNKNOWN
}
