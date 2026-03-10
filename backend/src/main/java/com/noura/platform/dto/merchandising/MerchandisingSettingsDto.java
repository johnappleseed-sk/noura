package com.noura.platform.dto.merchandising;

import java.util.UUID;

public record MerchandisingSettingsDto(
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
