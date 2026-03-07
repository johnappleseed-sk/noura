package com.noura.platform.dto.pricing;

import com.noura.platform.domain.enums.PriceListType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PriceListRequest(
        @NotBlank String name,
        @NotNull PriceListType type,
        UUID customerGroupId,
        UUID channelId
) {
}
