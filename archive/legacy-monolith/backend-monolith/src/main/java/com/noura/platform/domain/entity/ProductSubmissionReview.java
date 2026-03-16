package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.ProductSubmissionReviewAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable review events for product submission requests (approval history).
 */
@Getter
@Setter
@Entity
@Table(name = "product_submission_reviews")
public class ProductSubmissionReview extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id")
    private ProductSubmissionRequest submission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductSubmissionReviewAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id")
    private UserAccount reviewer;

    @Column(name = "reviewer_email")
    private String reviewerEmail;

    @Column(length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_product_id")
    private Product masterProduct;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}

