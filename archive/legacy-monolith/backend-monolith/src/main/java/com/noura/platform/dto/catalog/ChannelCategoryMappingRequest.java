package com.noura.platform.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChannelCategoryMappingRequest(
        @NotNull UUID categoryId,
        @NotBlank String channel,
        String regionCode,
        @NotBlank String externalCategoryId,
        String externalCategoryName,
        Boolean active
) {
}
