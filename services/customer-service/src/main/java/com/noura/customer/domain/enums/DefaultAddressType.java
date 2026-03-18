package com.noura.customer.domain.enums;

/**
 * Address default marker type used by set-default endpoint.
 */
public enum DefaultAddressType {
    /**
     * Set default shipping address only.
     */
    SHIPPING,
    /**
     * Set default billing address only.
     */
    BILLING,
    /**
     * Set both default shipping and billing address.
     */
    BOTH
}
