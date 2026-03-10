package com.noura.platform.dto.recommendation;

import java.util.List;
import java.util.UUID;

public record ProductRecommendationResponse(
        UUID productId,
        List<RecommendationProductDto> relatedProducts,
        List<RecommendationProductDto> frequentlyBoughtTogether,
        List<RecommendationProductDto> trendingProducts
) {
}
