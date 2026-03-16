package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.ProductSubmissionStatus;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Store-submitted proposal to create a new master product in Super Inventory.
 */
@Getter
@Setter
@Entity
@Table(name = "product_submission_requests")
public class ProductSubmissionRequest extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_submission_id")
    private ProductSubmissionRequest parentSubmission;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductSubmissionStatus status = ProductSubmissionStatus.PENDING_REVIEW;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Column(name = "normalized_name")
    private String normalizedName;

    @Column(length = 32)
    private String barcode;

    @Column(name = "manufacturer_part_number", length = 80)
    private String manufacturerPartNumber;

    @Column(name = "dedupe_fingerprint", length = 64)
    private String dedupeFingerprint;

    @Column(name = "potential_duplicate", nullable = false)
    private boolean potentialDuplicate = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_master_product_id")
    private Product matchedMasterProduct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id")
    private UserAccount requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private UserAccount reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;
}

