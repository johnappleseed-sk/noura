package com.noura.review.service;

import com.noura.review.domain.enums.ReviewModerationStatus;
import com.noura.review.dto.review.CreateReviewRequest;
import com.noura.review.dto.review.ModerateReviewRequest;
import com.noura.review.dto.review.RatingSummaryResponse;
import com.noura.review.dto.review.ReviewResponse;
import com.noura.review.service.model.ReviewRequestContext;

import java.util.List;
import java.util.UUID;

/**
 * Application service for storefront review submission, public review reads, rating aggregation,
 * and admin moderation actions.
 */
public interface ReviewService {

    /**
     * Returns product reviews for the caller-visible moderation scope.
     *
     * @param productId product identifier
     * @param context current actor context
     * @param moderationStatus optional moderation-status filter for moderators
     * @return review list
     */
    List<ReviewResponse> listProductReviews(
            UUID productId,
            ReviewRequestContext context,
            ReviewModerationStatus moderationStatus
    );

    /**
     * Creates one product review in a moderation-pending state.
     *
     * @param productId product identifier
     * @param request review submission payload
     * @param context current actor context
     * @return created review
     */
    ReviewResponse submitReview(
            UUID productId,
            CreateReviewRequest request,
            ReviewRequestContext context
    );

    /**
     * Returns approved-rating aggregates for one product.
     *
     * @param productId product identifier
     * @return rating summary
     */
    RatingSummaryResponse getRatingSummary(UUID productId);

    /**
     * Approves one review for storefront visibility.
     *
     * @param reviewId review identifier
     * @param request optional moderation note payload
     * @param context current actor context
     * @return updated review
     */
    ReviewResponse approveReview(
            UUID reviewId,
            ModerateReviewRequest request,
            ReviewRequestContext context
    );

    /**
     * Rejects one review and keeps it hidden from storefront reads.
     *
     * @param reviewId review identifier
     * @param request optional moderation note payload
     * @param context current actor context
     * @return updated review
     */
    ReviewResponse rejectReview(
            UUID reviewId,
            ModerateReviewRequest request,
            ReviewRequestContext context
    );
}
