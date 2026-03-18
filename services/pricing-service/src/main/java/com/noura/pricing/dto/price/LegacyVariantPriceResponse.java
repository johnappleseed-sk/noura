package com.noura.pricing.dto.price;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Legacy admin response returned after one variant-price upsert.
 *
 * @param id persisted price identifier
 * @param variantId variant identifier
 * @param priceListId price-list identifier
 * @param amount saved amount
 * @param currency currency code
 */
public record LegacyVariantPriceResponse(
        UUID id,
        UUID variantId,
        UUID priceListId,
        BigDecimal amount,
        String currency
) {
}
