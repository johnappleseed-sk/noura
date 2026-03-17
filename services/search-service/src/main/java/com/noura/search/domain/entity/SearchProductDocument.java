package com.noura.search.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Search-owned denormalized product document used by public search/discovery APIs.
 */
@Getter
@Setter
@Entity
@Table(name = "search_product_documents")
public class SearchProductDocument {

    @Id
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "product_code", length = 120)
    private String productCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", length = 255)
    private String slug;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "category_name", length = 255)
    private String categoryName;

    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    @Column(name = "short_description", length = 1000)
    private String shortDescription;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "trending", nullable = false)
    private boolean trending;

    @Column(name = "popularity_score", nullable = false)
    private int popularityScore;

    @Column(name = "average_rating", nullable = false)
    private double averageRating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(name = "indexed_at", nullable = false)
    private Instant indexedAt;

    /**
     * Normalizes mutable fields before insert/update.
     */
    @PrePersist
    @PreUpdate
    protected void normalize() {
        productCode = trimToNull(productCode);
        name = trimToNull(name);
        slug = trimToNull(slug);
        categoryName = trimToNull(categoryName);
        brandName = trimToNull(brandName);
        shortDescription = trimToNull(shortDescription);
        if (name == null) {
            throw new IllegalStateException("Search product document name is required");
        }
        if (popularityScore < 0) {
            popularityScore = 0;
        }
        if (averageRating < 0) {
            averageRating = 0;
        } else if (averageRating > 5) {
            averageRating = 5;
        }
        if (reviewCount < 0) {
            reviewCount = 0;
        }
        indexedAt = Instant.now();
    }

    /**
     * Trims source text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text or {@code null}
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
