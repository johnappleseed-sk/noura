package com.noura.platform.dto.pricing;

import com.noura.platform.domain.enums.PromotionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PromotionDto(
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
        List<PromotionApplicationItemDto> applications
) {
}
