package com.noura.pricing.dto.price;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Legacy variant-price upsert payload used by the admin pricing workspace.
 *
 * @param variantId variant identifier
 * @param priceListId legacy price-list identifier
 * @param amount sell price amount
 * @param currency ISO currency code
 * @param startDate optional activation start time
 * @param endDate optional activation end time
 * @param priority optional precedence
 */
public record LegacyVariantPriceUpsertRequest(
        @NotNull UUID variantId,
        @NotNull UUID priceListId,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal amount,
        @NotNull @Size(min = 3, max = 3) String currency,
        Instant startDate,
        Instant endDate,
        Integer priority
) {
}
