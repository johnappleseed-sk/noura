package com.noura.cart.dto.cart;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cart read model returned to storefront and admin clients.
 *
 * @param cartId cart identifier
 * @param ownerType owner type string (CUSTOMER or GUEST)
 * @param customerId customer identifier when owner is CUSTOMER
 * @param guestToken guest cart token when owner is GUEST
 * @param currencyCode cart currency code
 * @param storeId optional cart store scope
 * @param addressId optional delivery address scope placeholder
 * @param items cart line items
 * @param totals cart totals
 * @param itemCount aggregated quantity count
 * @param updatedAt cart update timestamp
 */
public record CartResponse(
        UUID cartId,
        String ownerType,
        String customerId,
        String guestToken,
        String currencyCode,
        UUID storeId,
        UUID addressId,
        List<CartItemResponse> items,
        CartTotalsResponse totals,
        int itemCount,
        Instant updatedAt
) {
}
