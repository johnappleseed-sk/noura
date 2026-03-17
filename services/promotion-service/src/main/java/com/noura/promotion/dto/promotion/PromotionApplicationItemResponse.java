package com.noura.promotion.dto.promotion;

import com.noura.promotion.domain.enums.PromotionApplicableEntityType;

import java.util.UUID;

/**
 * Scope mapping response for one promotion.
 *
 * @param applicableEntityType applicable entity type
 * @param applicableEntityId applicable entity identifier
 */
public record PromotionApplicationItemResponse(
        PromotionApplicableEntityType applicableEntityType,
        UUID applicableEntityId
) {
}
