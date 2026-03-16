package com.noura.platform.dto.recommendation;

import java.util.UUID;

public record RecommendationSettingsDto(
        UUID id,
        double productViewWeight,
        double addToCartWeight,
        double checkoutWeight,
        double trendingBoost,
        double bestSellerBoost,
        double ratingWeight,
        double categoryAffinityWeight,
        double brandAffinityWeight,
        double coPurchaseWeight,
        double dealBoost,
        int maxRecommendations
) {
}
