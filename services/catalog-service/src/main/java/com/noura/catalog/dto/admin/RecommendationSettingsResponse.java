package com.noura.catalog.dto.admin;

import java.util.UUID;

/**
 * Recommendation-control snapshot returned to admin-web.
 */
public record RecommendationSettingsResponse(
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
