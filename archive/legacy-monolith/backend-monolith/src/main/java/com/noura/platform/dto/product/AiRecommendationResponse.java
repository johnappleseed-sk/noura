package com.noura.platform.dto.product;

import java.time.Instant;
import java.util.List;

public record AiRecommendationResponse(
        String engine,
        String reason,
        Instant generatedAt,
        List<ProductDto> products
) {
}
