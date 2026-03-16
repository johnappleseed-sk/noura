package com.noura.cart.service.model;

import com.noura.cart.domain.enums.CartOwnerType;

/**
 * Request-level cart ownership context resolved from request headers.
 *
 * @param ownerType cart owner type
 * @param customerId customer identifier for CUSTOMER owner type
 * @param guestToken guest token for GUEST owner type
 */
public record CartContext(
        CartOwnerType ownerType,
        String customerId,
        String guestToken
) {

    /**
     * Creates customer cart context.
     *
     * @param customerId customer identifier
     * @return customer cart context
     */
    public static CartContext customer(String customerId) {
        return new CartContext(CartOwnerType.CUSTOMER, customerId, null);
    }

    /**
     * Creates guest cart context.
     *
     * @param guestToken guest cart token
     * @return guest cart context
     */
    public static CartContext guest(String guestToken) {
        return new CartContext(CartOwnerType.GUEST, null, guestToken);
    }

    /**
     * Returns stable actor identifier used for audit fields.
     *
     * @return actor identifier
     */
    public String actorId() {
        return ownerType == CartOwnerType.CUSTOMER ? customerId : guestToken;
    }
}
