package com.noura.cart.domain.enums;

/**
 * Lifecycle status of a cart aggregate.
 */
public enum CartStatus {
    /**
     * Cart can still be mutated.
     */
    ACTIVE,
    /**
     * Cart has been merged into another cart.
     */
    MERGED,
    /**
     * Cart has been used for checkout and is no longer mutable.
     */
    CHECKED_OUT
}
