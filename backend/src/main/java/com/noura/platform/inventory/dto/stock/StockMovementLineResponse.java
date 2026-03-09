package com.noura.platform.inventory.dto.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockMovementLineResponse(
        int lineNumber,
        String productId,
        String productSku,
        String productName,
        String batchId,
        String lotNumber,
        String fromBinId,
        String fromBinCode,
        String toBinId,
        String toBinCode,
        BigDecimal quantity,
        BigDecimal unitCost,
        LocalDate expiryDate,
        String notes
) {
}
