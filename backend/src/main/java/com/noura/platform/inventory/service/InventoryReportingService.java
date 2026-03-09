package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.report.InventoryTurnoverReportResponse;
import com.noura.platform.inventory.dto.report.LowStockReportItemResponse;
import com.noura.platform.inventory.dto.report.StockValuationReportResponse;

import java.time.Instant;
import java.util.List;

public interface InventoryReportingService {

    StockValuationReportResponse getStockValuationReport(String warehouseId, String productId);

    List<LowStockReportItemResponse> getLowStockReport(String warehouseId);

    InventoryTurnoverReportResponse getTurnoverReport(Instant dateFrom, Instant dateTo);

    byte[] exportCsv(String reportType, String warehouseId, String productId, Instant dateFrom, Instant dateTo);
}
