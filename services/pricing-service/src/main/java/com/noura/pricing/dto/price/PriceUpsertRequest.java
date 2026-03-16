package com.noura.pricing.dto.price;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Admin command to create or update a product price row by natural key.
 *
 * @param productId product identifier
 * @param currencyCode ISO currency code
 * @param basePrice base sell price
 * @param compareAtPrice optional compare-at (strikethrough) price
 * @param channelCode optional channel scope code
 * @param storeId optional store scope identifier
 * @param startsAt optional activation start time
 * @param endsAt optional activation end time
 * @param priority precedence inside same scope/window
 * @param active active flag
 */
public record PriceUpsertRequest(
        @NotNull UUID productId,
        @NotBlank @Size(min = 3, max = 3) String currencyCode,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal basePrice,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal compareAtPrice,
        @Size(max = 80) String channelCode,
        UUID storeId,
        Instant startsAt,
        Instant endsAt,
        Integer priority,
        Boolean active
) {
}

