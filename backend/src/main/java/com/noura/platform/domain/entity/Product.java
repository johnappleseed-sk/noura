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
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal basePrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private Map<String, Object> attributes = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.DRAFT;

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

    private String seoTitle;
    private String seoDescription;
    private String seoSlug;
}
