package com.noura.pricing.dto.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Resolved price view for storefront and checkout consumers.
 *
 * @param productId product identifier
 * @param currencyCode resolved currency code
 * @param basePrice resolved base price
 * @param compareAtPrice optional compare-at price
 * @param effectivePrice resolved effective sell price
 * @param sourcePriceId source price record identifier
 * @param channelCode channel scope of source record
 * @param storeId store scope of source record
 * @param startsAt source record activation start time
 * @param endsAt source record activation end time
 * @param priority source record priority
 * @param resolvedAt resolution timestamp
 */
public record PriceResolutionResponse(
        UUID productId,
        String currencyCode,
        BigDecimal basePrice,
        BigDecimal compareAtPrice,
        BigDecimal effectivePrice,
        UUID sourcePriceId,
        String channelCode,
        UUID storeId,
        Instant startsAt,
        Instant endsAt,
        int priority,
        Instant resolvedAt
) {
}

