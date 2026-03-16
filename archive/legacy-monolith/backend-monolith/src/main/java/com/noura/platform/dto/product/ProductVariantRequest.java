package com.noura.platform.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Map;

public record ProductVariantRequest(
        String color,
        String size,
        @NotBlank String sku,
        Map<String, Object> attributes,
        BigDecimal price,
        @PositiveOrZero Integer stock
) {
}
