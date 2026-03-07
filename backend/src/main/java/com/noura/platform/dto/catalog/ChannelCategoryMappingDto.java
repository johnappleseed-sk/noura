package com.noura.platform.dto.catalog;

import java.util.UUID;

public record ChannelCategoryMappingDto(
        UUID id,
        UUID categoryId,
        String channel,
        String regionCode,
        String externalCategoryId,
        String externalCategoryName,
        boolean active
) {
}
