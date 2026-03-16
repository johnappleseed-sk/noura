package com.noura.platform.service;

import com.noura.platform.dto.recommendation.ProductRecommendationResponse;
import com.noura.platform.dto.recommendation.RecommendationProductDto;

import java.util.List;
import java.util.UUID;

public interface RecommendationService {
    ProductRecommendationResponse getProductRecommendations(UUID productId, int limit);

    List<RecommendationProductDto> getTrendingRecommendations(int limit);

    List<RecommendationProductDto> getBestSellerRecommendations(int limit);

    List<RecommendationProductDto> getDealRecommendations(int limit);

    List<RecommendationProductDto> getPersonalizedRecommendations(String customerRef, int limit);

    List<RecommendationProductDto> getCrossSellRecommendations(String customerRef, int limit);
}
