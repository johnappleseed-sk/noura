package com.noura.platform.dto.pricing;

import com.noura.platform.domain.enums.PromotionApplicableEntityType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PromotionApplicationItemRequest(
        @NotNull PromotionApplicableEntityType applicableEntityType,
        @NotNull UUID applicableEntityId
) {
}
