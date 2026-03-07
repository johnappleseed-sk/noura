package com.noura.platform.dto.product;

import java.util.UUID;

public record ProductReviewDto(
        UUID id,
        UUID userId,
        String userName,
        int rating,
        String comment
) {
}
