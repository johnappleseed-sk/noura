package com.noura.pricing.dto.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response model for persisted product price records.
 *
 * @param id price record identifier
 * @param productId product identifier
 * @param currencyCode ISO currency code
 * @param basePrice base sell price
 * @param compareAtPrice optional compare-at price
 * @param effectivePrice effective price for current service policy
 * @param channelCode optional channel scope code
 * @param storeId optional store scope identifier
 * @param startsAt optional activation start time
 * @param endsAt optional activation end time
 * @param priority precedence inside same scope/window
 * @param active active flag
 * @param createdAt creation timestamp
 * @param updatedAt update timestamp
 */
public record ProductPriceResponse(
        UUID id,
        UUID productId,
        String currencyCode,
        BigDecimal basePrice,
        BigDecimal compareAtPrice,
        BigDecimal effectivePrice,
        String channelCode,
        UUID storeId,
        Instant startsAt,
        Instant endsAt,
        int priority,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

