package com.noura.catalog.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "product_variants")
public class CatalogProductVariant {

    @Id
    private UUID id;

    @Column(name = "product_id")
    private UUID productId;

    private String color;

    private String size;

    private String sku;

    @Column(name = "variant_name")
    private String variantName;

    private String barcode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new LinkedHashMap<>();

    @Column(name = "price_override")
    private BigDecimal priceOverride;

    private int stock;

    private boolean active;
}
