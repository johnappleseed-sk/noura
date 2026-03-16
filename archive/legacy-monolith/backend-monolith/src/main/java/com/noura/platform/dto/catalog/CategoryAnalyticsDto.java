package com.noura.platform.dto.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryAnalyticsDto(
        UUID categoryId,
        String categoryName,
        BigDecimal revenue,
        long unitsSold,
        long orderCount,
        long currentStock,
        BigDecimal inventoryTurnover,
        Double discoverabilityScore,
        Double conversionRate,
        Double profitMargin
) {
}
