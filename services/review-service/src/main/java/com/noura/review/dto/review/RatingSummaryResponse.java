package com.noura.review.dto.review;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aggregated rating summary for one product.
 *
 * @param productId product identifier
 * @param averageRating average approved rating
 * @param reviewCount approved review count
 * @param fiveStarCount count of approved five-star reviews
 * @param fourStarCount count of approved four-star reviews
 * @param threeStarCount count of approved three-star reviews
 * @param twoStarCount count of approved two-star reviews
 * @param oneStarCount count of approved one-star reviews
 */
public record RatingSummaryResponse(
        UUID productId,
        BigDecimal averageRating,
        int reviewCount,
        int fiveStarCount,
        int fourStarCount,
        int threeStarCount,
        int twoStarCount,
        int oneStarCount
) {
}
