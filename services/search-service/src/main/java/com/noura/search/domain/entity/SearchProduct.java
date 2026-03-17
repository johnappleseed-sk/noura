package com.noura.search.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only source projection mapped to the canonical catalog product table.
 *
 * <p>This entity is used only for index bootstrap/rebuild operations while search-service owns
 * its own `search_product_documents` projection table for query traffic.</p>
 */
@Getter
@Setter
@Immutable
@Entity
@Table(name = "products")
public class SearchProduct {

    @Id
    private UUID id;

    @Column(name = "product_code")
    private String productCode;

    private String name;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "brand_id")
    private UUID brandId;

    private boolean active;

    private boolean trending;

    @Column(name = "popularity_score")
    private int popularityScore;

    private String slug;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "average_rating")
    private double averageRating;

    @Column(name = "review_count")
    private int reviewCount;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
