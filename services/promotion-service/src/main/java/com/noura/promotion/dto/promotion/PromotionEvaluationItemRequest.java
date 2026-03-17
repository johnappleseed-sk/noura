package com.noura.promotion.dto.promotion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cart line item used for promotion evaluation.
 *
 * @param productId optional product identifier
 * @param categoryId optional category identifier
 * @param variantId optional variant identifier
 * @param quantity quantity
 * @param unitPrice unit price
 */
public record PromotionEvaluationItemRequest(
        UUID productId,
        UUID categoryId,
        UUID variantId,
        @Min(value = 1, message = "quantity must be at least 1")
        int quantity,
        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.00", message = "unitPrice must be greater than or equal to 0")
        BigDecimal unitPrice
) {
}
