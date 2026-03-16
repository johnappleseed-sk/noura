package com.noura.catalog.dto.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductStoreInventoryDto(
        UUID storeId,
        String storeName,
        int stock,
        BigDecimal storePrice,
        boolean published,
        boolean visible,
        String localName
) {
}
