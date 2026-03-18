package com.noura.catalog.dto.admin;

import java.util.UUID;

/**
 * Merchandising-control snapshot returned to admin-web.
 */
public record MerchandisingSettingsResponse(
        UUID id,
        double popularityWeight,
        double inventoryWeight,
        double impressionWeight,
        double clickWeight,
        double clickThroughRateWeight,
        double manualBoostWeight,
        int newArrivalWindowDays,
        double newArrivalBoost,
        double trendingBoost,
        double bestSellerBoost,
        double lowStockPenalty,
        int maxPageSize
) {
}
