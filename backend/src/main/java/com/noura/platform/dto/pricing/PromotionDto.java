package com.noura.platform.dto.pricing;

import com.noura.platform.domain.enums.PromotionType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PromotionDto(
        UUID id,
        String name,
        PromotionType type,
        String couponCode,
        Map<String, Object> conditions,
        Instant startDate,
        Instant endDate,
        boolean active,
        int priority,
        List<PromotionApplicationItemDto> applications
) {
}
