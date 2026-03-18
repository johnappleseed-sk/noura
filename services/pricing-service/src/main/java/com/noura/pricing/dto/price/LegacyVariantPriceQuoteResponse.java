package com.noura.pricing.dto.price;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Legacy admin quote response used by the pricing workspace.
 *
 * @param variantId variant identifier
 * @param currency currency code
 * @param baseAmount base price amount
 * @param finalAmount final resolved amount
 * @param appliedPromotionIds promotion identifiers, empty in the current compatibility slice
 */
public record LegacyVariantPriceQuoteResponse(
        UUID variantId,
        String currency,
        BigDecimal baseAmount,
        BigDecimal finalAmount,
        List<UUID> appliedPromotionIds
) {
}
