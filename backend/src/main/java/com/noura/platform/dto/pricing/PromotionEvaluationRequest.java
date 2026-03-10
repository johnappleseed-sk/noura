package com.noura.platform.dto.pricing;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

public record PromotionEvaluationRequest(
        BigDecimal subtotal,
        String couponCode,
        String customerSegment,
        List<@Valid PromotionEvaluationItemRequest> items
) {
}
