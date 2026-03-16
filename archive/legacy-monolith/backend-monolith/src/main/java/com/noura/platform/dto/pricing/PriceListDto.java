package com.noura.platform.dto.pricing;

import com.noura.platform.domain.enums.PriceListType;

import java.util.UUID;

public record PriceListDto(
        UUID id,
        String name,
        PriceListType type,
        UUID customerGroupId,
        UUID channelId
) {
}
