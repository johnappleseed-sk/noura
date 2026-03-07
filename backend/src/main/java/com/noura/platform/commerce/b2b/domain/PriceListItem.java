package com.noura.platform.commerce.b2b.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Individual price item in a price list.
 * Can be a fixed price or a percentage discount.
 */
@Entity
@Table(name = "b2b_price_list_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"price_list_id", "product_id", "variant_id"}))
public class PriceListItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id", nullable = false)
    private PriceList priceList;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Optional variant ID for variant-specific pricing
    @Column(name = "variant_id")
    private Long variantId;

    // Pricing type
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriceType priceType = PriceType.FIXED;

    // Fixed price (when priceType = FIXED)
    @Column(precision = 15, scale = 4)
    private BigDecimal fixedPrice;

    // Discount percentage off standard price (when priceType = DISCOUNT_PERCENT)
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercent;

    // Minimum quantity for this price (for volume pricing)
    @Column(nullable = false)
    private int minimumQuantity = 1;

    // === Getters and Setters ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PriceList getPriceList() {
        return priceList;
    }

    public void setPriceList(PriceList priceList) {
        this.priceList = priceList;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public void setPriceType(PriceType priceType) {
        this.priceType = priceType;
    }

    public BigDecimal getFixedPrice() {
        return fixedPrice;
    }

    public void setFixedPrice(BigDecimal fixedPrice) {
        this.fixedPrice = fixedPrice;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public int getMinimumQuantity() {
        return minimumQuantity;
    }

    public void setMinimumQuantity(int minimumQuantity) {
        this.minimumQuantity = minimumQuantity;
    }
}
