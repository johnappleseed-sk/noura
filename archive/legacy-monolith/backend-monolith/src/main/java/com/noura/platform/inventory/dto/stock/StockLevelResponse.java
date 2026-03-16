package com.noura.platform.inventory.dto.stock;

import java.math.BigDecimal;
import java.time.Instant;

public record StockLevelResponse(
        String id,
        String productId,
        String productSku,
        String productName,
        String warehouseId,
        String warehouseCode,
        String warehouseName,
        String binId,
        String binCode,
        String batchId,
        String lotNumber,
        BigDecimal quantityOnHand,
        BigDecimal quantityReserved,
        BigDecimal quantityAvailable,
        BigDecimal quantityDamaged,
        Instant lastMovementAt,
        boolean lowStock,
        BigDecimal lowStockThreshold,
        Instant updatedAt
) {
}
