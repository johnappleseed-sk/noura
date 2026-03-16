package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.ApprovalDecisionType;
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

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "product_approval_decisions")
public class ProductApprovalDecision extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id")
    private ProductSubmission submission;

    @Column(name = "submission_id", insertable = false, updatable = false)
    private UUID submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 40)
    private ApprovalDecisionType decisionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_product_id")
    private Product targetProduct;

    @Column(name = "target_product_id", insertable = false, updatable = false)
    private UUID targetProductId;

    @Column(length = 1000)
    private String notes;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @Column(name = "decided_by", nullable = false, length = 255)
    private String decidedBy;

    @PrePersist
    @PreUpdate
    void normalize() {
        notes = trim(notes);
        decidedBy = trim(decidedBy);
        if (decidedAt == null) {
            decidedAt = Instant.now();
        }
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
