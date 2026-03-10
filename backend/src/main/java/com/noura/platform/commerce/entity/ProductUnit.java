package com.noura.platform.commerce.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "product_unit", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_unit_product_name", columnNames = {"product_id", "name"}),
        @UniqueConstraint(name = "uk_product_unit_product_barcode", columnNames = {"product_id", "barcode"})
})
public class ProductUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 32)
    private String abbreviation;

    @Column(name = "conversion_to_base", nullable = false, precision = 19, scale = 6)
    private BigDecimal conversionToBase;

    @Column(name = "allow_for_sale", nullable = false)
    private Boolean allowForSale = true;

    @Column(name = "allow_for_purchase", nullable = false)
    private Boolean allowForPurchase = true;

    @Column(name = "is_default_sale_unit", nullable = false)
    private Boolean isDefaultSaleUnit = false;

    @Column(name = "is_default_purchase_unit", nullable = false)
    private Boolean isDefaultPurchaseUnit = false;

    @Column(length = 128)
    private String barcode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        normalize();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (name != null) name = name.trim();
        if (abbreviation != null) {
            abbreviation = abbreviation.trim();
            if (abbreviation.isEmpty()) abbreviation = null;
        }
        if (barcode != null) {
            barcode = barcode.trim();
            if (barcode.isEmpty()) barcode = null;
        }
        if (conversionToBase == null || conversionToBase.compareTo(BigDecimal.ZERO) <= 0) {
            conversionToBase = BigDecimal.ONE;
        }
        if (allowForSale == null) allowForSale = true;
        if (allowForPurchase == null) allowForPurchase = true;
        if (isDefaultSaleUnit == null) isDefaultSaleUnit = false;
        if (isDefaultPurchaseUnit == null) isDefaultPurchaseUnit = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public BigDecimal getConversionToBase() {
        return conversionToBase;
    }

    public void setConversionToBase(BigDecimal conversionToBase) {
        this.conversionToBase = conversionToBase;
    }

    public Boolean getAllowForSale() {
        return allowForSale;
    }

    public void setAllowForSale(Boolean allowForSale) {
        this.allowForSale = allowForSale;
    }

    public Boolean getAllowForPurchase() {
        return allowForPurchase;
    }

    public void setAllowForPurchase(Boolean allowForPurchase) {
        this.allowForPurchase = allowForPurchase;
    }

    public Boolean getIsDefaultSaleUnit() {
        return isDefaultSaleUnit;
    }

    public void setIsDefaultSaleUnit(Boolean isDefaultSaleUnit) {
        this.isDefaultSaleUnit = isDefaultSaleUnit;
    }

    public Boolean getIsDefaultPurchaseUnit() {
        return isDefaultPurchaseUnit;
    }

    public void setIsDefaultPurchaseUnit(Boolean isDefaultPurchaseUnit) {
        this.isDefaultPurchaseUnit = isDefaultPurchaseUnit;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
