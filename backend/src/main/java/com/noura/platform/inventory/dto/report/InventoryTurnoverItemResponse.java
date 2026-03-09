package com.noura.platform.inventory.dto.report;

import java.math.BigDecimal;

public record InventoryTurnoverItemResponse(
        String productId,
        String productSku,
        String productName,
        BigDecimal outboundQuantity,
        BigDecimal currentAvailable,
        BigDecimal turnoverRatio
) {
}
