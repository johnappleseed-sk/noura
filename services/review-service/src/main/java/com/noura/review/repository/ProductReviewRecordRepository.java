package com.noura.review.repository;

import com.noura.review.domain.entity.ProductReviewRecord;
import com.noura.review.domain.enums.ReviewModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for product review persistence and moderation lookups.
 */
public interface ProductReviewRecordRepository extends JpaRepository<ProductReviewRecord, UUID> {

    /**
     * Finds all reviews for one product ordered by submission time descending.
     *
     * @param productId product identifier
     * @return review list
     */
    List<ProductReviewRecord> findByProductIdOrderByCreatedAtDesc(UUID productId);

    /**
     * Finds product reviews filtered by moderation status.
     *
     * @param productId product identifier
     * @param moderationStatus moderation status
     * @return review list
     */
    List<ProductReviewRecord> findByProductIdAndModerationStatusOrderByCreatedAtDesc(
            UUID productId,
            ReviewModerationStatus moderationStatus
    );

    /**
     * Finds a review previously submitted by one customer for one product.
     *
     * @param productId product identifier
     * @param customerRef customer identifier
     * @return matching review when present
     */
    Optional<ProductReviewRecord> findByProductIdAndCustomerRef(UUID productId, String customerRef);
}
