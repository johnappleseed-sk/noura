package com.noura.platform.dto.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductInventoryDto(
        UUID id,
        UUID productId,
        UUID storeId,
        int stock,
        BigDecimal storePrice
) {
}
