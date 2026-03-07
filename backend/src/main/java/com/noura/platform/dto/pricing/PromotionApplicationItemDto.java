package com.noura.platform.dto.pricing;

import com.noura.platform.domain.enums.PromotionApplicableEntityType;

import java.util.UUID;

public record PromotionApplicationItemDto(
        PromotionApplicableEntityType applicableEntityType,
        UUID applicableEntityId
) {
}
