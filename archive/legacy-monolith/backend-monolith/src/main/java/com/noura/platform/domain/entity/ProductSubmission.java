package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.SubmissionStatus;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "product_submissions")
public class ProductSubmission extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "merchant_id", insertable = false, updatable = false)
    private UUID merchantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "store_id", insertable = false, updatable = false)
    private UUID storeId;

    @Column(name = "proposed_name", nullable = false, length = 255)
    private String proposedName;

    @Column(name = "proposed_brand", length = 255)
    private String proposedBrand;

    @Column(name = "proposed_category_code", length = 80)
    private String proposedCategoryCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_attributes_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> proposedAttributesJson = new LinkedHashMap<>();

    @Column(name = "proposed_barcode", length = 64)
    private String proposedBarcode;

    @Column(name = "proposed_sku", length = 120)
    private String proposedSku;

    @Column(name = "similarity_hash", nullable = false, length = 64)
    private String similarityHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SubmissionStatus status = SubmissionStatus.PENDING_REVIEW;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    @PrePersist
    @PreUpdate
    void normalize() {
        proposedName = trim(proposedName);
        proposedBrand = trim(proposedBrand);
        proposedCategoryCode = uppercase(trim(proposedCategoryCode));
        proposedBarcode = trim(proposedBarcode);
        proposedSku = uppercase(trim(proposedSku));
        reviewedBy = trim(reviewedBy);
        reviewNotes = trim(reviewNotes);
        similarityHash = trim(similarityHash);
        if (status == null) {
            status = SubmissionStatus.PENDING_REVIEW;
        }
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
        if (proposedAttributesJson == null) {
            proposedAttributesJson = new LinkedHashMap<>();
        }
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String uppercase(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
