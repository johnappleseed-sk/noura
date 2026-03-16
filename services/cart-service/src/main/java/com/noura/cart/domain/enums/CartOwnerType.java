package com.noura.cart.domain.enums;

/**
 * Cart owner identity type.
 */
public enum CartOwnerType {
    /**
     * Authenticated storefront customer cart.
     */
    CUSTOMER,
    /**
     * Guest storefront cart keyed by guest token.
     */
    GUEST
}
