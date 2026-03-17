package com.noura.review.dto.review;

import com.noura.review.domain.enums.ReviewModerationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Product review response returned by public and moderation APIs.
 *
 * @param id review identifier
 * @param productId product identifier
 * @param customerName review author display name
 * @param rating star rating
 * @param title optional review title
 * @param comment review body
 * @param moderationStatus moderation state
 * @param createdAt submission timestamp
 * @param moderatedAt moderation timestamp
 */
public record ReviewResponse(
        UUID id,
        UUID productId,
        String customerName,
        int rating,
        String title,
        String comment,
        ReviewModerationStatus moderationStatus,
        Instant createdAt,
        Instant moderatedAt
) {
}
