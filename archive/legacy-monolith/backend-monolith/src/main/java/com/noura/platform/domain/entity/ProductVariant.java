package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "product_variants")
public class ProductVariant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_id", insertable = false, updatable = false)
    private UUID productId;

    @Column
    private String color;

    @Column
    private String size;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(name = "variant_name", length = 255)
    private String variantName;

    @Column(length = 64)
    private String barcode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> attributes = new LinkedHashMap<>();

    @Column(name = "price_override", precision = 14, scale = 2)
    private BigDecimal priceOverride;

    @Column(nullable = false)
    private int stock = 0;

    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    @PreUpdate
    void normalize() {
        color = trim(color);
        size = trim(size);
        sku = normalizeSku(sku);
        variantName = normalizeVariantName(variantName, color, size, sku);
        barcode = trim(barcode);
    }

    private String normalizeSku(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            return "SKU-" + randomSuffix().toUpperCase(Locale.ROOT);
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "SKU-" + randomSuffix().toUpperCase(Locale.ROOT) : normalized;
    }

    private String normalizeVariantName(String value, String variantColor, String variantSize, String variantSku) {
        String normalized = trim(value);
        if (normalized != null) {
            return normalized;
        }
        normalized = trim(String.join(" ", safePart(variantColor), safePart(variantSize)).trim());
        if (normalized != null) {
            return normalized;
        }
        return variantSku;
    }

    private String safePart(String value) {
        return value == null ? "" : value.trim();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
