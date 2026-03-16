package com.noura.platform.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDto(
        BigDecimal revenue,
        long ordersCount,
        long usersCount,
        long storesCount,
        List<String> topProducts,
        List<String> storePerformance
) {
}
