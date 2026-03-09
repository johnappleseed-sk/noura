package com.noura.platform.inventory.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record StockValuationReportResponse(
        BigDecimal totalStockValue,
        List<StockValuationItemResponse> items
) {
}
