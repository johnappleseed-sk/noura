package com.noura.promotion.dto.promotion;

import com.noura.promotion.domain.enums.PromotionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Admin mutation payload for creating or updating promotions.
 *
 * @param name promotion name
 * @param type promotion type
 * @param code optional business code
 * @param description optional description
 * @param couponCode optional coupon or promo code
 * @param conditions deterministic rule conditions
 * @param startDate optional start date
 * @param endDate optional end date
 * @param active whether promotion is active
 * @param stackable whether promotion can stack
 * @param priority evaluation priority
 * @param usageLimitTotal optional total usage limit
 * @param usageLimitPerCustomer optional per-customer limit placeholder
 * @param customerSegment optional exact-match customer segment
 * @param archived whether promotion is archived
 * @param applications optional scope mappings
 */
public record PromotionUpsertRequest(
        @NotBlank(message = "name is required")
        String name,
        @NotNull(message = "type is required")
        PromotionType type,
        String code,
        String description,
        String couponCode,
        Map<String, Object> conditions,
        Instant startDate,
        Instant endDate,
        Boolean active,
        Boolean stackable,
        Integer priority,
        Integer usageLimitTotal,
        Integer usageLimitPerCustomer,
        String customerSegment,
        Boolean archived,
        List<@Valid PromotionApplicationItemRequest> applications
) {
}
