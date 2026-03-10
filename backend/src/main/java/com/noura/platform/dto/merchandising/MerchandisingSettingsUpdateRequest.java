package com.noura.platform.dto.merchandising;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MerchandisingSettingsUpdateRequest(
        @NotNull @DecimalMin("0.0") Double popularityWeight,
        @NotNull @DecimalMin("0.0") Double inventoryWeight,
        @NotNull @DecimalMin("0.0") Double impressionWeight,
        @NotNull @DecimalMin("0.0") Double clickWeight,
        @NotNull @DecimalMin("0.0") Double clickThroughRateWeight,
        @NotNull @DecimalMin("0.0") Double manualBoostWeight,
        @NotNull @Min(1) @Max(365) Integer newArrivalWindowDays,
        @NotNull @DecimalMin("0.0") Double newArrivalBoost,
        @NotNull @DecimalMin("0.0") Double trendingBoost,
        @NotNull @DecimalMin("0.0") Double bestSellerBoost,
        @NotNull @DecimalMin("0.0") Double lowStockPenalty,
        @NotNull @Min(6) @Max(96) Integer maxPageSize
) {
}
