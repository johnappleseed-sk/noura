package com.noura.catalog.domain.entity;

import com.noura.catalog.domain.enums.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "products")
public class CatalogProduct {

    @Id
    private UUID id;

    @Column(name = "product_code")
    private String productCode;

    private String name;

    private String slug;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Column(name = "approval_status")
    private String approvalStatus;

    private boolean active;

    @Column(name = "allow_backorder")
    private boolean allowBackorder;

    @Column(name = "flash_sale")
    private boolean flashSale;

    private boolean trending;

    @Column(name = "best_seller")
    private boolean bestSeller;

    @Column(name = "average_rating")
    private double averageRating;

    @Column(name = "review_count")
    private int reviewCount;

    @Column(name = "popularity_score")
    private int popularityScore;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "long_description")
    private String longDescription;

    @Column(name = "target_audience")
    private String targetAudience;

    private String barcode;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "seo_title")
    private String seoTitle;

    @Column(name = "seo_description")
    private String seoDescription;

    @Column(name = "seo_slug")
    private String seoSlug;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
