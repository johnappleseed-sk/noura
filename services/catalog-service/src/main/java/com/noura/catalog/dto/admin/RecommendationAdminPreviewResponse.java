package com.noura.catalog.dto.admin;

import java.util.List;
import java.util.UUID;

/**
 * Full recommendation-control preview used by admin-web.
 */
public record RecommendationAdminPreviewResponse(
        RecommendationSettingsResponse settings,
        String customerRef,
        UUID productId,
        List<RecommendationAdminProductDto> trending,
        List<RecommendationAdminProductDto> bestSellers,
        List<RecommendationAdminProductDto> deals,
        List<RecommendationAdminProductDto> personalized,
        List<RecommendationAdminProductDto> crossSell,
        RecommendationProductPreviewResponse productPreview
) {
}
