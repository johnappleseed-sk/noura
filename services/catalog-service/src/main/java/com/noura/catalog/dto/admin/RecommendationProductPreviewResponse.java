package com.noura.catalog.dto.admin;

import java.util.List;
import java.util.UUID;

/**
 * Product-scoped recommendation preview grouping used by admin-web.
 */
public record RecommendationProductPreviewResponse(
        UUID productId,
        List<RecommendationAdminProductDto> relatedProducts,
        List<RecommendationAdminProductDto> frequentlyBoughtTogether
) {
}
