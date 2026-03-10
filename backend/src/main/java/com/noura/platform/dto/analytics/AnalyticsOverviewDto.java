package com.noura.platform.dto.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnalyticsOverviewDto(
        Instant from,
        Instant to,
        long totalEvents,
        long productViews,
        long addToCartCount,
        long removeFromCartCount,
        long checkoutStartedCount,
        long checkoutCompletedCount,
        long promotionAppliedCount,
        BigDecimal conversionRate,
        BigDecimal cartAbandonmentRate,
        BigDecimal averageOrderValue,
        Map<String, Long> eventTypeCounts,
        List<AnalyticsEventDto> recentEvents
) {
}
