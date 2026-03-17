package com.noura.promotion.dto.promotion;

import com.noura.promotion.domain.enums.PromotionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Promotion response DTO for public and admin promotion APIs.
 *
 * @param id promotion identifier
 * @param name promotion name
 * @param code optional business code
 * @param description optional description
 * @param type promotion type
 * @param couponCode optional promo code
 * @param conditions deterministic rule conditions
 * @param startDate start date
 * @param endDate end date
 * @param active whether promotion is active
 * @param stackable whether promotion stacks
 * @param priority evaluation priority
 * @param usageLimitTotal total usage limit
 * @param usageLimitPerCustomer per-customer limit placeholder
 * @param usageCount current usage count
 * @param customerSegment required customer segment
 * @param archived whether archived
 * @param discountPercent resolved display percent from conditions
 * @param discountAmount resolved display amount from conditions
 * @param applications scope mappings
 */
public record PromotionResponse(
        UUID id,
        String name,
        String code,
        String description,
        PromotionType type,
        String couponCode,
        Map<String, Object> conditions,
        Instant startDate,
        Instant endDate,
        boolean active,
        boolean stackable,
        int priority,
        Integer usageLimitTotal,
        Integer usageLimitPerCustomer,
        int usageCount,
        String customerSegment,
        boolean archived,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        List<PromotionApplicationItemResponse> applications
) {
}
