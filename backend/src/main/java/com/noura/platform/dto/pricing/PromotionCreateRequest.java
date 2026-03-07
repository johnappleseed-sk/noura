package com.noura.platform.dto.pricing;

import com.noura.platform.domain.enums.PromotionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PromotionCreateRequest(
        @NotBlank String name,
        @NotNull PromotionType type,
        String couponCode,
        Map<String, Object> conditions,
        Instant startDate,
        Instant endDate,
        Boolean active,
        Integer priority,
        List<@Valid PromotionApplicationItemRequest> applications
) {
}
