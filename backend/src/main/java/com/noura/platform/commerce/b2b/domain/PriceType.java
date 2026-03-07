package com.noura.platform.commerce.b2b.domain;

/**
 * Type of pricing in a price list item.
 */
public enum PriceType {
    /** Fixed price override */
    FIXED,

    /** Percentage discount off standard price */
    DISCOUNT_PERCENT,

    /** Fixed amount discount off standard price */
    DISCOUNT_AMOUNT
}
