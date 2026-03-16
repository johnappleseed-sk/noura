package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.ProductStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_code", length = 80)
    private String productCode;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "category_id", insertable = false, updatable = false)
    private UUID categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "brand_id", insertable = false, updatable = false)
    private UUID brandId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal basePrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> attributes = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "approval_status", length = 40)
    private String approvalStatus;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "allow_backorder", nullable = false)
    private boolean allowBackorder = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(nullable = false)
    private boolean flashSale;

    @Column(nullable = false)
    private boolean trending;

    @Column(nullable = false)
    private boolean bestSeller;

    @Column(nullable = false)
    private double averageRating = 0D;

    @Column(nullable = false)
    private int reviewCount = 0;

    @Column(nullable = false)
    private int popularityScore = 0;

    @Column(length = 600)
    private String shortDescription;

    @Column(length = 5000)
    private String longDescription;

    @Column(name = "target_audience", length = 255)
    private String targetAudience;

    @Column(name = "barcode", length = 32)
    private String barcode;

    @Column(name = "manufacturer_part_number", length = 80)
    private String manufacturerPartNumber;

    @Column(name = "normalized_name", length = 255)
    private String normalizedName;

    @Column(name = "dedupe_fingerprint", length = 64)
    private String dedupeFingerprint;

    @Column(name = "qr_code", length = 1024)
    private String qrCode;

    private String seoTitle;
    private String seoDescription;
    private String seoSlug;

    @PrePersist
    @PreUpdate
    void normalize() {
        name = trim(name);
        shortDescription = trim(shortDescription);
        longDescription = trim(longDescription);
        targetAudience = trim(targetAudience);
        barcode = trim(barcode);
        manufacturerPartNumber = trim(manufacturerPartNumber);
        productCode = normalizeCode(productCode);
        slug = normalizeSlug(slug, seoSlug, name);
        if (seoSlug == null || seoSlug.isBlank()) {
            seoSlug = slug;
        } else {
            seoSlug = normalizeExistingSlug(seoSlug);
        }
        if (approvalStatus == null || approvalStatus.isBlank()) {
            approvalStatus = "PENDING";
        } else {
            approvalStatus = approvalStatus.trim().toUpperCase(Locale.ROOT);
        }
        if (normalizedName == null || normalizedName.isBlank()) {
            normalizedName = normalizeProductName(name);
        }
    }

    private String normalizeCode(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            return "PRD-" + randomSuffix().toUpperCase(Locale.ROOT);
        }
        normalized = normalized.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "PRD-" + randomSuffix().toUpperCase(Locale.ROOT) : normalized;
    }

    private String normalizeSlug(String value, String seoFallback, String nameFallback) {
        String seed = trim(value);
        boolean generated = false;
        if (seed == null) {
            seed = trim(seoFallback);
            generated = true;
        }
        if (seed == null) {
            seed = trim(nameFallback);
            generated = true;
        }
        if (seed == null) {
            seed = "product";
            generated = true;
        }
        String normalized = normalizeExistingSlug(seed);
        if (normalized == null) {
            normalized = "product";
            generated = true;
        }
        if (generated) {
            return normalized + "-" + randomSuffix().toLowerCase(Locale.ROOT);
        }
        return normalized;
    }

    private String normalizeExistingSlug(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeProductName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
