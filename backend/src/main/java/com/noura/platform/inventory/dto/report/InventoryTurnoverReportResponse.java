package com.noura.platform.inventory.dto.report;

import java.time.Instant;
import java.util.List;

public record InventoryTurnoverReportResponse(
        Instant dateFrom,
        Instant dateTo,
        List<InventoryTurnoverItemResponse> items
) {
}
