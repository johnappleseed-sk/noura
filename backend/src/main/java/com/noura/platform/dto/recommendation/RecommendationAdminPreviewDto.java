package com.noura.platform.dto.recommendation;

import java.util.List;
import java.util.UUID;

public record RecommendationAdminPreviewDto(
        RecommendationSettingsDto settings,
        String customerRef,
        UUID productId,
        List<RecommendationProductDto> trending,
        List<RecommendationProductDto> bestSellers,
        List<RecommendationProductDto> deals,
        List<RecommendationProductDto> personalized,
        List<RecommendationProductDto> crossSell,
        ProductRecommendationResponse productPreview
) {
}
