package com.noura.platform.dto.product;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ProductVariantDto(
        UUID id,
        String color,
        String size,
        String sku,
        Map<String, Object> attributes,
        BigDecimal priceOverride,
        int stock,
        boolean active
) {
}
