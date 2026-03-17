package com.noura.review.controller;

import com.noura.review.common.ApiResponse;
import com.noura.review.controller.support.ReviewRequestContextResolver;
import com.noura.review.domain.enums.ReviewModerationStatus;
import com.noura.review.dto.review.CreateReviewRequest;
import com.noura.review.dto.review.ModerateReviewRequest;
import com.noura.review.dto.review.RatingSummaryResponse;
import com.noura.review.dto.review.ReviewResponse;
import com.noura.review.service.ReviewService;
import com.noura.review.service.model.ReviewRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for storefront review reads/submissions and admin moderation actions.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRequestContextResolver contextResolver;

    /**
     * Returns product reviews visible to the current actor.
     *
     * @param productId product identifier
     * @param moderationStatus optional moderator-only filter
     * @param request current request
     * @return review list
     */
    @GetMapping({
            "/api/v1/products/{productId}/reviews",
            "/api/products/{productId}/reviews"
    })
    public ApiResponse<List<ReviewResponse>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(required = false) ReviewModerationStatus moderationStatus,
            HttpServletRequest request
    ) {
        ReviewRequestContext context = contextResolver.resolve(request);
        List<ReviewResponse> data = reviewService.listProductReviews(productId, context, moderationStatus);
        return ApiResponse.ok("Product reviews", data, request.getRequestURI());
    }

    /**
     * Creates one product review in a pending moderation state.
     *
     * @param productId product identifier
     * @param requestBody submission payload
     * @param request current request
     * @return created review
     */
    @PostMapping({
            "/api/v1/products/{productId}/reviews",
            "/api/products/{productId}/reviews"
    })
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateReviewRequest requestBody,
            HttpServletRequest request
    ) {
        ReviewRequestContext context = contextResolver.resolve(request);
        ReviewResponse data = reviewService.submitReview(productId, requestBody, context);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Review submitted for moderation", data, request.getRequestURI()));
    }

    /**
     * Returns approved rating aggregates for one product.
     *
     * @param productId product identifier
     * @param request current request
     * @return rating summary
     */
    @GetMapping({
            "/api/v1/products/{productId}/rating-summary",
            "/api/products/{productId}/rating-summary"
    })
    public ApiResponse<RatingSummaryResponse> getRatingSummary(
            @PathVariable UUID productId,
            HttpServletRequest request
    ) {
        RatingSummaryResponse data = reviewService.getRatingSummary(productId);
        return ApiResponse.ok("Product rating summary", data, request.getRequestURI());
    }

    /**
     * Approves one review for public visibility.
     *
     * @param reviewId review identifier
     * @param requestBody optional moderation note payload
     * @param request current request
     * @return moderated review
     */
    @PostMapping({
            "/api/v1/admin/reviews/{reviewId}/approve",
            "/api/admin/reviews/{reviewId}/approve"
    })
    public ApiResponse<ReviewResponse> approveReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody(required = false) ModerateReviewRequest requestBody,
            HttpServletRequest request
    ) {
        ReviewRequestContext context = contextResolver.resolve(request);
        ReviewResponse data = reviewService.approveReview(reviewId, requestBody, context);
        return ApiResponse.ok("Review approved", data, request.getRequestURI());
    }

    /**
     * Rejects one review and keeps it hidden from storefront reads.
     *
     * @param reviewId review identifier
     * @param requestBody optional moderation note payload
     * @param request current request
     * @return moderated review
     */
    @PostMapping({
            "/api/v1/admin/reviews/{reviewId}/reject",
            "/api/admin/reviews/{reviewId}/reject"
    })
    public ApiResponse<ReviewResponse> rejectReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody(required = false) ModerateReviewRequest requestBody,
            HttpServletRequest request
    ) {
        ReviewRequestContext context = contextResolver.resolve(request);
        ReviewResponse data = reviewService.rejectReview(reviewId, requestBody, context);
        return ApiResponse.ok("Review rejected", data, request.getRequestURI());
    }
}
