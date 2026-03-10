package com.noura.platform.dto.recommendation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecommendationSettingsUpdateRequest(
        @NotNull @DecimalMin("0.0") Double productViewWeight,
        @NotNull @DecimalMin("0.0") Double addToCartWeight,
        @NotNull @DecimalMin("0.0") Double checkoutWeight,
        @NotNull @DecimalMin("0.0") Double trendingBoost,
        @NotNull @DecimalMin("0.0") Double bestSellerBoost,
        @NotNull @DecimalMin("0.0") Double ratingWeight,
        @NotNull @DecimalMin("0.0") Double categoryAffinityWeight,
        @NotNull @DecimalMin("0.0") Double brandAffinityWeight,
        @NotNull @DecimalMin("0.0") Double coPurchaseWeight,
        @NotNull @DecimalMin("0.0") Double dealBoost,
        @NotNull @Min(1) @Max(24) Integer maxRecommendations
) {
}
