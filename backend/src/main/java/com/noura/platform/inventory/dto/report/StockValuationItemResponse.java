package com.noura.platform.inventory.dto.report;

import java.math.BigDecimal;

public record StockValuationItemResponse(
        String productId,
        String productSku,
        String productName,
        String warehouseId,
        String warehouseCode,
        String binId,
        String binCode,
        BigDecimal quantityAvailable,
        BigDecimal unitPrice,
        BigDecimal stockValue
) {
}
