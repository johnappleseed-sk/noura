package com.noura.review.domain.entity;

import com.noura.review.domain.enums.ReviewModerationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Review aggregate root storing one customer's product rating and moderation state.
 */
@Getter
@Setter
@Entity
@Table(name = "product_reviews")
public class ProductReviewRecord extends AuditableEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "customer_ref", nullable = false, length = 180)
    private String customerRef;

    @Column(name = "customer_name", length = 180)
    private String customerName;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "title", length = 180)
    private String title;

    @Column(name = "comment", nullable = false, length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 24)
    private ReviewModerationStatus moderationStatus = ReviewModerationStatus.PENDING;

    @Column(name = "moderation_notes", length = 1000)
    private String moderationNotes;

    @Column(name = "submission_ip_hash", length = 64)
    private String submissionIpHash;

    @Column(name = "submission_user_agent_hash", length = 64)
    private String submissionUserAgentHash;

    @Column(name = "spam_signals_json", nullable = false)
    private String spamSignalsJson = "{}";

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "moderated_by", length = 180)
    private String moderatedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    /**
     * Normalizes mutable fields before insert/update.
     */
    @PrePersist
    @PreUpdate
    protected void normalize() {
        customerRef = trimToNull(customerRef);
        customerName = normalizeDisplayName(customerName);
        title = trimToNull(title);
        comment = trimToNull(comment);
        moderationNotes = trimToNull(moderationNotes);
        moderatedBy = trimToNull(moderatedBy);
        spamSignalsJson = trimToNull(spamSignalsJson);
        if (spamSignalsJson == null) {
            spamSignalsJson = "{}";
        }
        if (rating < 1) {
            rating = 1;
        } else if (rating > 5) {
            rating = 5;
        }
        if (moderationStatus == null) {
            moderationStatus = ReviewModerationStatus.PENDING;
        }
    }

    /**
     * Trims source text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Normalizes customer display names while keeping a simple human-readable fallback.
     *
     * @param value source display name
     * @return normalized display name
     */
    private String normalizeDisplayName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "Customer";
        }
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 180).trim();
    }
}
