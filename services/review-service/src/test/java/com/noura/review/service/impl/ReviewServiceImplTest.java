package com.noura.review.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.review.domain.entity.ProductReviewRecord;
import com.noura.review.domain.enums.ReviewModerationStatus;
import com.noura.review.dto.review.CreateReviewRequest;
import com.noura.review.dto.review.ModerateReviewRequest;
import com.noura.review.dto.review.RatingSummaryResponse;
import com.noura.review.dto.review.ReviewResponse;
import com.noura.review.exception.ReviewOperationException;
import com.noura.review.integration.client.CatalogServiceClient;
import com.noura.review.repository.ProductReviewRecordRepository;
import com.noura.review.service.model.ReviewRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReviewServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ProductReviewRecordRepository productReviewRecordRepository;

    @Mock
    private CatalogServiceClient catalogServiceClient;

    @Captor
    private ArgumentCaptor<ProductReviewRecord> reviewCaptor;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                productReviewRecordRepository,
                catalogServiceClient,
                new ObjectMapper()
        );
    }

    /**
     * Verifies review submission creates one pending record with privacy-safe spam metadata.
     */
    @Test
    void shouldCreatePendingReviewOnSubmission() {
        UUID productId = UUID.randomUUID();
        ReviewRequestContext context = customerContext();
        when(productReviewRecordRepository.findByProductIdAndCustomerRef(productId, "customer-1"))
                .thenReturn(Optional.empty());
        when(catalogServiceClient.getProductById(eq(context), any(), eq(productId)))
                .thenReturn(new CatalogServiceClient.ProductPayload(productId, "Travel Mug", true));
        when(productReviewRecordRepository.save(any(ProductReviewRecord.class)))
                .thenAnswer(invocation -> {
                    ProductReviewRecord review = invocation.getArgument(0);
                    review.setId(UUID.randomUUID());
                    review.setCreatedAt(Instant.parse("2026-03-17T10:15:30Z"));
                    review.setUpdatedAt(Instant.parse("2026-03-17T10:15:30Z"));
                    return review;
                });

        ReviewResponse response = reviewService.submitReview(
                productId,
                new CreateReviewRequest(5, "Excellent", "Very clean build quality."),
                context
        );

        verify(productReviewRecordRepository).save(reviewCaptor.capture());
        ProductReviewRecord saved = reviewCaptor.getValue();
        Assertions.assertEquals(ReviewModerationStatus.PENDING, saved.getModerationStatus());
        Assertions.assertEquals("customer-1", saved.getCustomerRef());
        Assertions.assertEquals("Noura Shopper", saved.getCustomerName());
        Assertions.assertNotNull(saved.getSubmissionIpHash());
        Assertions.assertNotNull(saved.getSubmissionUserAgentHash());
        Assertions.assertTrue(saved.getSpamSignalsJson().contains("\"moderationRequired\":true"));
        Assertions.assertEquals(ReviewModerationStatus.PENDING, response.moderationStatus());
    }

    /**
     * Verifies anonymous callers cannot submit reviews.
     */
    @Test
    void shouldRejectAnonymousReviewSubmission() {
        ReviewRequestContext context = new ReviewRequestContext(
                null,
                null,
                null,
                Set.of(),
                false,
                null,
                null
        );

        ReviewOperationException ex = Assertions.assertThrows(
                ReviewOperationException.class,
                () -> reviewService.submitReview(UUID.randomUUID(), new CreateReviewRequest(4, null, "Solid"), context)
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(productReviewRecordRepository, never()).save(any());
    }

    /**
     * Verifies duplicate customer reviews are rejected before persistence.
     */
    @Test
    void shouldRejectDuplicateReviewSubmission() {
        UUID productId = UUID.randomUUID();
        when(productReviewRecordRepository.findByProductIdAndCustomerRef(productId, "customer-1"))
                .thenReturn(Optional.of(new ProductReviewRecord()));

        ReviewOperationException ex = Assertions.assertThrows(
                ReviewOperationException.class,
                () -> reviewService.submitReview(productId, new CreateReviewRequest(4, null, "Solid"), customerContext())
        );

        Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        Assertions.assertEquals("REVIEW_ALREADY_EXISTS", ex.getCode());
        verify(catalogServiceClient, never()).getProductById(any(), any(), any());
    }

    /**
     * Verifies public review reads remain restricted to approved reviews.
     */
    @Test
    void shouldReturnApprovedReviewsForPublicReads() {
        UUID productId = UUID.randomUUID();
        when(productReviewRecordRepository.findByProductIdAndModerationStatusOrderByCreatedAtDesc(
                productId,
                ReviewModerationStatus.APPROVED
        )).thenReturn(List.of(review(productId, 5, ReviewModerationStatus.APPROVED)));

        List<ReviewResponse> reviews = reviewService.listProductReviews(
                productId,
                customerContext(),
                null
        );

        Assertions.assertEquals(1, reviews.size());
        Assertions.assertEquals(ReviewModerationStatus.APPROVED, reviews.getFirst().moderationStatus());
    }

    /**
     * Verifies moderators can request pending reviews explicitly.
     */
    @Test
    void shouldAllowModeratorToReadPendingReviews() {
        UUID productId = UUID.randomUUID();
        when(productReviewRecordRepository.findByProductIdAndModerationStatusOrderByCreatedAtDesc(
                productId,
                ReviewModerationStatus.PENDING
        )).thenReturn(List.of(review(productId, 3, ReviewModerationStatus.PENDING)));

        List<ReviewResponse> reviews = reviewService.listProductReviews(
                productId,
                moderatorContext(),
                ReviewModerationStatus.PENDING
        );

        Assertions.assertEquals(1, reviews.size());
        Assertions.assertEquals(ReviewModerationStatus.PENDING, reviews.getFirst().moderationStatus());
    }

    /**
     * Verifies approved-review aggregates ignore pending reviews.
     */
    @Test
    void shouldAggregateApprovedRatingsOnly() {
        UUID productId = UUID.randomUUID();
        when(productReviewRecordRepository.findByProductIdAndModerationStatusOrderByCreatedAtDesc(
                productId,
                ReviewModerationStatus.APPROVED
        )).thenReturn(List.of(
                review(productId, 5, ReviewModerationStatus.APPROVED),
                review(productId, 4, ReviewModerationStatus.APPROVED),
                review(productId, 4, ReviewModerationStatus.APPROVED)
        ));

        RatingSummaryResponse summary = reviewService.getRatingSummary(productId);

        Assertions.assertEquals(3, summary.reviewCount());
        Assertions.assertEquals(0, summary.oneStarCount());
        Assertions.assertEquals(1, summary.fiveStarCount());
        Assertions.assertEquals(2, summary.fourStarCount());
        Assertions.assertEquals("4.33", summary.averageRating().toPlainString());
    }

    /**
     * Verifies approval updates moderation audit fields.
     */
    @Test
    void shouldApproveReview() {
        UUID reviewId = UUID.randomUUID();
        ProductReviewRecord review = review(UUID.randomUUID(), 5, ReviewModerationStatus.PENDING);
        review.setId(reviewId);
        when(productReviewRecordRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(productReviewRecordRepository.save(any(ProductReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.approveReview(
                reviewId,
                new ModerateReviewRequest("Clean and legitimate"),
                moderatorContext()
        );

        Assertions.assertEquals(ReviewModerationStatus.APPROVED, response.moderationStatus());
        Assertions.assertNotNull(review.getApprovedAt());
        Assertions.assertEquals("moderator-1", review.getModeratedBy());
    }

    /**
     * Verifies rejection is blocked for non-moderators.
     */
    @Test
    void shouldRejectModerationWithoutPrivileges() {
        ReviewOperationException ex = Assertions.assertThrows(
                ReviewOperationException.class,
                () -> reviewService.rejectReview(UUID.randomUUID(), new ModerateReviewRequest("spam"), customerContext())
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    private ReviewRequestContext customerContext() {
        return new ReviewRequestContext(
                "customer-1",
                "Noura Shopper",
                "Bearer token",
                Set.of("CUSTOMER"),
                false,
                "203.0.113.10",
                "Mozilla/5.0"
        );
    }

    private ReviewRequestContext moderatorContext() {
        return new ReviewRequestContext(
                "moderator-1",
                "Ops Moderator",
                "Bearer token",
                Set.of("ROLE_MODERATOR"),
                false,
                "203.0.113.20",
                "Mozilla/5.0"
        );
    }

    private ProductReviewRecord review(UUID productId, int rating, ReviewModerationStatus status) {
        ProductReviewRecord review = new ProductReviewRecord();
        review.setId(UUID.randomUUID());
        review.setProductId(productId);
        review.setCustomerRef("customer-" + rating);
        review.setCustomerName("Customer " + rating);
        review.setRating(rating);
        review.setComment("Comment " + rating);
        review.setModerationStatus(status);
        review.setCreatedAt(Instant.parse("2026-03-17T09:00:00Z"));
        review.setUpdatedAt(Instant.parse("2026-03-17T09:00:00Z"));
        return review;
    }
}
