package com.noura.platform.dto.pricing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PromotionEvaluationItemRequest(
        @NotNull UUID productId,
        UUID categoryId,
        @Min(1) int quantity,
        @NotNull BigDecimal unitPrice
) {
}
