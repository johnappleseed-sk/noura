package com.noura.platform.commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@SQLRestriction("deleted_status = 0")
@Table(indexes = {
        @Index(name = "idx_product_name", columnList = "name"),
        @Index(name = "idx_product_active", columnList = "active"),
        @Index(name = "idx_product_active_id", columnList = "active,id"),
        @Index(name = "idx_product_active_category_id", columnList = "active,category_id,id"),
        @Index(name = "idx_product_allow_negative_stock", columnList = "allow_negative_stock"),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_price", columnList = "price"),
        @Index(name = "idx_product_stock_qty", columnList = "stock_qty"),
        @Index(name = "idx_product_low_stock_threshold", columnList = "low_stock_threshold"),
        @Index(name = "idx_product_deleted_status", columnList = "deleted_status"),
        @Index(name = "idx_product_expiration_date", columnList = "expiration_date")
})
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String sku;

    @Column(unique = true)
    private String barcode;

    private String name;
    private BigDecimal price;
    private BigDecimal wholesalePrice;
    private Integer wholesaleMinQty;
    private BigDecimal costPrice;
    private Integer stockQty;
    private Integer lowStockThreshold;
    private Integer unitsPerBox;
    private Integer unitsPerCase;
    @Column(length = 64)
    private String baseUnitName;
    @Column(nullable = false)
    private Integer baseUnitPrecision = 0;
    private Long retailPriceUnitId;
    private Long wholesalePriceUnitId;
    private Long wholesaleMinQtyUnitId;
    private Long lowStockThresholdUnitId;
    private Boolean active = true;
    @Column(nullable = false)
    private Boolean allowNegativeStock = false;
    @Column(length = 2048)
    private String imageUrl;
    @Column(length = 1024)
    private String boxSpecifications;
    @Column(precision = 12, scale = 3)
    private BigDecimal weightValue;
    @Column(length = 16)
    private String weightUnit;
    @Column(precision = 12, scale = 3)
    private BigDecimal lengthValue;
    @Column(length = 16)
    private String lengthUnit;
    @Column(precision = 12, scale = 3)
    private BigDecimal widthValue;
    @Column(length = 16)
    private String widthUnit;
    @Column(precision = 12, scale = 3)
    private BigDecimal heightValue;
    @Column(length = 16)
    private String heightUnit;
    @Column(length = 32)
    private String basicUnit;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expirationDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate manufactureDate;
    @Column(nullable = false)
    private Boolean deletedStatus = false;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedAt;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("conversionToBase ASC, id ASC")
    private List<ProductUnit> productUnits = new ArrayList<>();

    /**
     * Executes the isLowStock operation.
     *
     * @return {@code boolean} Result produced by this operation.
     * <p>Possible exceptions: Runtime exceptions from downstream dependencies may propagate unchanged.</p>
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    /**
     * Executes the isLowStock operation.
     *
     * @return {@code boolean} Result produced by this operation.
     * <p>Possible exceptions: Runtime exceptions from downstream dependencies may propagate unchanged.</p>
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    /**
     * Executes the isLowStock operation.
     *
     * @return {@code boolean} Result produced by this operation.
     * <p>Possible exceptions: Runtime exceptions from downstream dependencies may propagate unchanged.</p>
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    @Transient
    public boolean isLowStock() {
        if (stockQty == null || lowStockThreshold == null) return false;
        return stockQty <= lowStockThreshold;
    }

    /**
     * Executes the onCreate operation.
     *
     * @return void No value is returned; the method applies side effects to existing state.
     * <p>Possible exceptions: Runtime exceptions from downstream dependencies may propagate unchanged.</p>
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    /**
     * Executes the onCreate operation.
     *
     * @return void No value is returned; the method applies side effects to existing state.
     * <p>Possible exceptions: Runtime exceptions from downstream dependencies may propagate unchanged.</p>
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    /**
     * Executes the onCreate operation.
     *
     * @return void No value is returned; the method applies side effects to existing state.
     * <p>Possible exceptions: Runtime exceptions from downstream dependencies may propagate unchanged.</p>
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    @PrePersist
    public void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (deletedStatus == null) {
            deletedStatus = false;
        }
        if (Boolean.TRUE.equals(deletedStatus) && deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
        if (baseUnitName == null || baseUnitName.trim().isEmpty()) {
            if (basicUnit != null && !basicUnit.trim().isEmpty()) {
                baseUnitName = basicUnit.trim();
            } else {
                baseUnitName = "piece";
            }
        }
        if (baseUnitPrecision == null || baseUnitPrecision < 0) {
            baseUnitPrecision = 0;
        }
    }

    /**
     * Executes the onUpdate operation.
     *
     * @return void No value is returned; the method applies side effects to existing state.
     * <p>Possible exceptions: Runtime exceptions from downstream dependencies may propagate unchanged.</p>
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    /**
     * Executes the onUpdate operation.
     *
     * @return void No value is returned; the method applies side effects to existing state.
     * <p>Possible exceptions: Runtime exceptions from downstream dependencies may propagate unchanged.</p>
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    /**
     * Executes the onUpdate operation.
     *
     * @return void No value is returned; the method applies side effects to existing state.
     * <p>Possible exceptions: Runtime exceptions from downstream dependencies may propagate unchanged.</p>
     * <p>Edge cases: Null, empty, and boundary inputs are handled by the existing control flow and validations.</p>
     */
    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (Boolean.TRUE.equals(deletedStatus)) {
            if (deletedAt == null) {
                deletedAt = LocalDateTime.now();
            }
            if (active == null || active) {
                active = false;
            }
        } else {
            deletedAt = null;
        }
        if (baseUnitName == null || baseUnitName.trim().isEmpty()) {
            if (basicUnit != null && !basicUnit.trim().isEmpty()) {
                baseUnitName = basicUnit.trim();
            } else {
                baseUnitName = "piece";
            }
        } else {
            baseUnitName = baseUnitName.trim();
        }
        if (baseUnitPrecision == null || baseUnitPrecision < 0) {
            baseUnitPrecision = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getWholesalePrice() {
        return wholesalePrice;
    }

    public void setWholesalePrice(BigDecimal wholesalePrice) {
        this.wholesalePrice = wholesalePrice;
    }

    public Integer getWholesaleMinQty() {
        return wholesaleMinQty;
    }

    public void setWholesaleMinQty(Integer wholesaleMinQty) {
        this.wholesaleMinQty = wholesaleMinQty;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public Integer getStockQty() {
        return stockQty;
    }

    public void setStockQty(Integer stockQty) {
        this.stockQty = stockQty;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Integer getUnitsPerBox() {
        return unitsPerBox;
    }

    public void setUnitsPerBox(Integer unitsPerBox) {
        this.unitsPerBox = unitsPerBox;
    }

    public Integer getUnitsPerCase() {
        return unitsPerCase;
    }

    public void setUnitsPerCase(Integer unitsPerCase) {
        this.unitsPerCase = unitsPerCase;
    }

    public String getBaseUnitName() {
        return baseUnitName;
    }

    public void setBaseUnitName(String baseUnitName) {
        this.baseUnitName = baseUnitName;
    }

    public Integer getBaseUnitPrecision() {
        return baseUnitPrecision;
    }

    public void setBaseUnitPrecision(Integer baseUnitPrecision) {
        this.baseUnitPrecision = baseUnitPrecision;
    }

    public Long getRetailPriceUnitId() {
        return retailPriceUnitId;
    }

    public void setRetailPriceUnitId(Long retailPriceUnitId) {
        this.retailPriceUnitId = retailPriceUnitId;
    }

    public Long getWholesalePriceUnitId() {
        return wholesalePriceUnitId;
    }

    public void setWholesalePriceUnitId(Long wholesalePriceUnitId) {
        this.wholesalePriceUnitId = wholesalePriceUnitId;
    }

    public Long getWholesaleMinQtyUnitId() {
        return wholesaleMinQtyUnitId;
    }

    public void setWholesaleMinQtyUnitId(Long wholesaleMinQtyUnitId) {
        this.wholesaleMinQtyUnitId = wholesaleMinQtyUnitId;
    }

    public Long getLowStockThresholdUnitId() {
        return lowStockThresholdUnitId;
    }

    public void setLowStockThresholdUnitId(Long lowStockThresholdUnitId) {
        this.lowStockThresholdUnitId = lowStockThresholdUnitId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getAllowNegativeStock() {
        return allowNegativeStock;
    }

    public void setAllowNegativeStock(Boolean allowNegativeStock) {
        this.allowNegativeStock = allowNegativeStock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBoxSpecifications() {
        return boxSpecifications;
    }

    public void setBoxSpecifications(String boxSpecifications) {
        this.boxSpecifications = boxSpecifications;
    }

    public BigDecimal getWeightValue() {
        return weightValue;
    }

    public void setWeightValue(BigDecimal weightValue) {
        this.weightValue = weightValue;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public BigDecimal getLengthValue() {
        return lengthValue;
    }

    public void setLengthValue(BigDecimal lengthValue) {
        this.lengthValue = lengthValue;
    }

    public String getLengthUnit() {
        return lengthUnit;
    }

    public void setLengthUnit(String lengthUnit) {
        this.lengthUnit = lengthUnit;
    }

    public BigDecimal getWidthValue() {
        return widthValue;
    }

    public void setWidthValue(BigDecimal widthValue) {
        this.widthValue = widthValue;
    }

    public String getWidthUnit() {
        return widthUnit;
    }

    public void setWidthUnit(String widthUnit) {
        this.widthUnit = widthUnit;
    }

    public BigDecimal getHeightValue() {
        return heightValue;
    }

    public void setHeightValue(BigDecimal heightValue) {
        this.heightValue = heightValue;
    }

    public String getHeightUnit() {
        return heightUnit;
    }

    public void setHeightUnit(String heightUnit) {
        this.heightUnit = heightUnit;
    }

    public String getBasicUnit() {
        return basicUnit;
    }

    public void setBasicUnit(String basicUnit) {
        this.basicUnit = basicUnit;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(LocalDate manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public Boolean getDeletedStatus() {
        return deletedStatus;
    }

    public void setDeletedStatus(Boolean deletedStatus) {
        this.deletedStatus = deletedStatus;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<ProductUnit> getProductUnits() {
        return productUnits;
    }

    public void setProductUnits(List<ProductUnit> productUnits) {
        this.productUnits = productUnits;
    }
}
