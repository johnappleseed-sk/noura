package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dedupe candidate rows associated with a product submission for reviewer decisioning.
 */
@Getter
@Setter
@Entity
@Table(name = "product_dedupe_candidates")
public class ProductDedupeCandidate extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id")
    private ProductSubmissionRequest submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_product_id")
    private Product masterProduct;

    @Column(name = "match_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal matchScore = BigDecimal.ZERO;

    @Column(name = "match_reason", nullable = false, length = 120)
    private String matchReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> detail = new LinkedHashMap<>();
}

