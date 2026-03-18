package com.noura.pricing.dto.price;

import java.util.UUID;

/**
 * Legacy price-list response used by the admin pricing workspace.
 *
 * @param id price-list identifier
 * @param name display name
 * @param type list type
 * @param customerGroupId optional customer-group scope
 * @param channelId optional channel scope
 */
public record LegacyPriceListResponse(
        UUID id,
        String name,
        String type,
        UUID customerGroupId,
        UUID channelId
) {
}
