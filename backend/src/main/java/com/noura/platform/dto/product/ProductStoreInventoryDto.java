package com.noura.platform.dto.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductStoreInventoryDto(
        UUID storeId,
        String storeName,
        int stock,
        BigDecimal storePrice
) {
}
