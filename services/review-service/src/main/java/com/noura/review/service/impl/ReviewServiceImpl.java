package com.noura.review.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.review.domain.entity.ProductReviewRecord;
import com.noura.review.domain.enums.ReviewModerationStatus;
import com.noura.review.dto.review.CreateReviewRequest;
import com.noura.review.dto.review.ModerateReviewRequest;
import com.noura.review.dto.review.RatingSummaryResponse;
import com.noura.review.dto.review.ReviewResponse;
import com.noura.review.exception.NotFoundException;
import com.noura.review.exception.ReviewOperationException;
import com.noura.review.integration.client.CatalogServiceClient;
import com.noura.review.repository.ProductReviewRecordRepository;
import com.noura.review.service.ReviewService;
import com.noura.review.service.model.ReviewRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link ReviewService}.
 *
 * <p>The first review-service slice keeps moderation deterministic and easy to reason about:
 * one review per customer per product, pending-first publication, approved-only storefront reads,
 * and simple aggregate calculations derived from persisted approved reviews.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final BigDecimal ZERO_RATING = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ProductReviewRecordRepository productReviewRecordRepository;
    private final CatalogServiceClient catalogServiceClient;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> listProductReviews(
            UUID productId,
            ReviewRequestContext context,
            ReviewModerationStatus moderationStatus
    ) {
        ReviewModerationStatus effectiveStatus = resolveVisibleStatus(context, moderationStatus);
        return productReviewRecordRepository
                .findByProductIdAndModerationStatusOrderByCreatedAtDesc(productId, effectiveStatus)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ReviewResponse submitReview(
            UUID productId,
            CreateReviewRequest request,
            ReviewRequestContext context
    ) {
        assertAuthenticatedCustomer(context);

        String customerRef = normalizeRequired(context.subject());
        if (productReviewRecordRepository.findByProductIdAndCustomerRef(productId, customerRef).isPresent()) {
            throw new ReviewOperationException(
                    HttpStatus.CONFLICT,
                    "REVIEW_ALREADY_EXISTS",
                    "Customer has already submitted a review for this product"
            );
        }

        CatalogServiceClient.ProductPayload product = catalogServiceClient.getProductById(
                context,
                MDC.get(CORRELATION_ID_MDC_KEY),
                productId
        );
        assertProductCanReceiveReviews(productId, product);

        ProductReviewRecord review = new ProductReviewRecord();
        review.setProductId(productId);
        review.setCustomerRef(customerRef);
        review.setCustomerName(context.reviewAuthorName());
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setComment(request.comment());
        review.setModerationStatus(ReviewModerationStatus.PENDING);
        review.setSubmissionIpHash(hashNullable(context.remoteAddress()));
        review.setSubmissionUserAgentHash(hashNullable(context.userAgent()));
        review.setSpamSignalsJson(buildSpamSignalsJson(context));
        review.setCreatedBy(context.actorId());
        review.setUpdatedBy(context.actorId());

        try {
            ProductReviewRecord saved = productReviewRecordRepository.save(review);
            log.info("Created pending review {} for product {} by {}", saved.getId(), productId, customerRef);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ReviewOperationException(
                    HttpStatus.CONFLICT,
                    "REVIEW_ALREADY_EXISTS",
                    "Customer has already submitted a review for this product"
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(UUID productId) {
        List<ProductReviewRecord> approvedReviews = loadApprovedReviews(productId);
        int[] ratingBuckets = new int[6];
        int total = 0;
        for (ProductReviewRecord review : approvedReviews) {
            int rating = normalizeRating(review.getRating());
            ratingBuckets[rating]++;
            total += rating;
        }

        BigDecimal averageRating = approvedReviews.isEmpty()
                ? ZERO_RATING
                : BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(approvedReviews.size()), 2, RoundingMode.HALF_UP);

        return new RatingSummaryResponse(
                productId,
                averageRating,
                approvedReviews.size(),
                ratingBuckets[5],
                ratingBuckets[4],
                ratingBuckets[3],
                ratingBuckets[2],
                ratingBuckets[1]
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ReviewResponse approveReview(
            UUID reviewId,
            ModerateReviewRequest request,
            ReviewRequestContext context
    ) {
        return moderateReview(reviewId, ReviewModerationStatus.APPROVED, request, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ReviewResponse rejectReview(
            UUID reviewId,
            ModerateReviewRequest request,
            ReviewRequestContext context
    ) {
        return moderateReview(reviewId, ReviewModerationStatus.REJECTED, request, context);
    }

    /**
     * Applies one moderation outcome. The service permits status overrides so operations can reverse
     * earlier decisions without deleting or recreating review records.
     *
     * @param reviewId review identifier
     * @param targetStatus desired moderation state
     * @param request optional moderation request
     * @param context current actor context
     * @return updated review
     */
    private ReviewResponse moderateReview(
            UUID reviewId,
            ReviewModerationStatus targetStatus,
            ModerateReviewRequest request,
            ReviewRequestContext context
    ) {
        assertCanModerate(context);
        ProductReviewRecord review = requireReview(reviewId);

        Instant now = Instant.now();
        review.setModerationStatus(targetStatus);
        review.setModerationNotes(request == null ? null : request.moderationNotes());
        review.setModeratedAt(now);
        review.setModeratedBy(context.actorId());
        review.setUpdatedBy(context.actorId());

        if (targetStatus == ReviewModerationStatus.APPROVED) {
            if (review.getApprovedAt() == null) {
                review.setApprovedAt(now);
            }
            review.setRejectedAt(null);
        } else {
            if (review.getRejectedAt() == null) {
                review.setRejectedAt(now);
            }
            review.setApprovedAt(null);
        }

        ProductReviewRecord saved = productReviewRecordRepository.save(review);
        log.info("Moderated review {} to {} by {}", reviewId, targetStatus, context.actorId());
        return toResponse(saved);
    }

    /**
     * Resolves which moderation scope the current actor can read.
     *
     * @param context current actor context
     * @param requestedStatus optional query filter
     * @return effective visible status
     */
    private ReviewModerationStatus resolveVisibleStatus(
            ReviewRequestContext context,
            ReviewModerationStatus requestedStatus
    ) {
        if (requestedStatus == null || requestedStatus == ReviewModerationStatus.APPROVED) {
            return ReviewModerationStatus.APPROVED;
        }
        assertCanModerate(context);
        return requestedStatus;
    }

    /**
     * Loads one existing review or throws a stable not-found error.
     *
     * @param reviewId review identifier
     * @return review record
     */
    private ProductReviewRecord requireReview(UUID reviewId) {
        return productReviewRecordRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND", "Review was not found"));
    }

    /**
     * Loads approved reviews for one product ordered by newest submission first.
     *
     * @param productId product identifier
     * @return approved review list
     */
    private List<ProductReviewRecord> loadApprovedReviews(UUID productId) {
        return productReviewRecordRepository.findByProductIdAndModerationStatusOrderByCreatedAtDesc(
                productId,
                ReviewModerationStatus.APPROVED
        );
    }

    /**
     * Validates that the current actor is an authenticated customer or trusted service acting with a subject.
     *
     * @param context current actor context
     */
    private void assertAuthenticatedCustomer(ReviewRequestContext context) {
        if (context == null || !context.hasSubject()) {
            throw new ReviewOperationException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTHENTICATION_REQUIRED",
                    "Review submission requires an authenticated customer"
            );
        }
    }

    /**
     * Validates moderator privileges.
     *
     * @param context current actor context
     */
    private void assertCanModerate(ReviewRequestContext context) {
        if (context == null || !context.canModerateReviews()) {
            throw new ReviewOperationException(
                    HttpStatus.FORBIDDEN,
                    "REVIEW_FORBIDDEN",
                    "Review moderation requires admin or moderator permissions"
            );
        }
    }

    /**
     * Validates that catalog-service resolved the same product and that it remains active.
     *
     * @param productId requested product identifier
     * @param product product payload from catalog-service
     */
    private void assertProductCanReceiveReviews(UUID productId, CatalogServiceClient.ProductPayload product) {
        if (product == null || product.id() == null || !productId.equals(product.id())) {
            throw new ReviewOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PRODUCT_LOOKUP_INVALID",
                    "Catalog service returned an inconsistent product response"
            );
        }
        if (!product.active()) {
            throw new ReviewOperationException(
                    HttpStatus.CONFLICT,
                    "PRODUCT_REVIEW_UNAVAILABLE",
                    "Reviews are unavailable for inactive products"
            );
        }
    }

    /**
     * Maps one persisted review to the public/admin response contract.
     *
     * @param review review record
     * @return response DTO
     */
    private ReviewResponse toResponse(ProductReviewRecord review) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getCustomerName(),
                normalizeRating(review.getRating()),
                review.getTitle(),
                review.getComment(),
                review.getModerationStatus(),
                review.getCreatedAt(),
                review.getModeratedAt()
        );
    }

    /**
     * Produces a stable JSON payload with privacy-safe submission signals. The first slice stores
     * only hashed transport metadata so moderation/spam tooling can evolve later without retaining
     * raw IP or user-agent values.
     *
     * @param context current actor context
     * @return serialized JSON document
     */
    private String buildSpamSignalsJson(ReviewRequestContext context) {
        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("moderationRequired", true);
        signals.put("submissionSource", context != null && context.internalCall() ? "internal" : "storefront");
        signals.put("ipHashPresent", context != null && context.remoteAddress() != null && !context.remoteAddress().isBlank());
        signals.put("userAgentHashPresent", context != null && context.userAgent() != null && !context.userAgent().isBlank());
        signals.put("roleCodes", sortedRoles(context));
        try {
            return objectMapper.writeValueAsString(signals);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize review spam signals: {}", ex.getMessage());
            return "{}";
        }
    }

    /**
     * Returns sorted role codes for stable persistence and testability.
     *
     * @param context current actor context
     * @return sorted role list
     */
    private List<String> sortedRoles(ReviewRequestContext context) {
        if (context == null || context.roles() == null || context.roles().isEmpty()) {
            return List.of();
        }
        return context.roles().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /**
     * Hashes privacy-sensitive text fields before persistence.
     *
     * @param value source text
     * @return SHA-256 digest or {@code null}
     */
    private String hashNullable(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    /**
     * Trims an optional string and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Trims a required string and rejects blanks.
     *
     * @param value source text
     * @return normalized text
     */
    private String normalizeRequired(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ReviewOperationException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTHENTICATION_REQUIRED",
                    "Review submission requires an authenticated customer"
            );
        }
        return normalized;
    }

    /**
     * Normalizes ratings into the persisted bounds used by the migration constraint.
     *
     * @param rating source rating
     * @return safe rating value
     */
    private int normalizeRating(int rating) {
        if (rating < 1) {
            return 1;
        }
        if (rating > 5) {
            return 5;
        }
        return rating;
    }
}
