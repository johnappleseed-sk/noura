package com.noura.promotion.dto.promotion;

import com.noura.promotion.domain.enums.PromotionApplicableEntityType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Scope mapping request for one promotion.
 *
 * @param applicableEntityType applicable entity type
 * @param applicableEntityId applicable entity identifier
 */
public record PromotionApplicationItemRequest(
        @NotNull(message = "applicableEntityType is required")
        PromotionApplicableEntityType applicableEntityType,
        @NotNull(message = "applicableEntityId is required")
        UUID applicableEntityId
) {
}
