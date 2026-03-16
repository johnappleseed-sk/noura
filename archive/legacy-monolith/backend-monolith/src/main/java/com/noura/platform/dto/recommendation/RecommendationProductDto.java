package com.noura.platform.dto.recommendation;

import java.math.BigDecimal;
import java.util.UUID;

public record RecommendationProductDto(
        UUID id,
        String name,
        UUID categoryId,
        String categoryName,
        BigDecimal price,
        String imageUrl,
        String shortDescription,
        double score,
        String reason
) {
}
