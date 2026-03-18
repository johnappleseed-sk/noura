package com.noura.pricing.dto.price;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Legacy price-list creation payload used by the admin pricing workspace.
 *
 * @param name display name
 * @param type list type such as {@code BASE} or {@code CHANNEL}
 * @param customerGroupId optional customer-group scope
 * @param channelId optional channel scope
 */
public record LegacyPriceListRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 32) String type,
        UUID customerGroupId,
        UUID channelId
) {
}
