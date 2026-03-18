package com.noura.checkout.dto.checkout;

import java.util.UUID;

/**
 * Checkout validation issue record.
 *
 * @param code stable machine-readable issue code
 * @param detail human-readable issue detail
 * @param productId affected product identifier, when issue is line-specific
 * @param lineItemId affected cart line identifier, when issue is line-specific
 */
public record CheckoutIssueResponse(
        String code,
        String detail,
        UUID productId,
        UUID lineItemId
) {
}

