package com.noura.platform.dto.pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PromotionEvaluationDto(
        BigDecimal subtotal,
        BigDecimal discountAmount,
        boolean freeShipping,
        List<UUID> appliedPromotionIds,
        List<String> appliedPromotionCodes
) {
}
