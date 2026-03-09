package com.noura.platform.inventory.dto.report;

import java.math.BigDecimal;

public record LowStockReportItemResponse(
        String productId,
        String productSku,
        String productName,
        String warehouseId,
        String warehouseCode,
        BigDecimal quantityAvailable,
        BigDecimal lowStockThreshold,
        BigDecimal reorderPoint,
        BigDecimal reorderQuantity
) {
}
