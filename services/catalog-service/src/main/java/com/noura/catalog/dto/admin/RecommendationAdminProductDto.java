package com.noura.catalog.dto.admin;

import java.util.UUID;

/**
 * Lightweight recommendation preview item tailored to the current admin-web page contract.
 */
public record RecommendationAdminProductDto(
        UUID id,
        String name,
        String categoryName,
        double score,
        String reason
) {
}
