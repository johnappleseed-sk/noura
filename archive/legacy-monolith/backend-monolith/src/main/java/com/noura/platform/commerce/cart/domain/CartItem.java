package com.noura.platform.commerce.cart.domain;

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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "customer_cart_item")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id")
    private Long productId;

    @Column(length = 128)
    private String sku;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(name = "unit_label", length = 64)
    private String unitLabel;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        normalize();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (productName == null || productName.isBlank()) {
            productName = "";
        } else {
            productName = productName.trim();
        }
        if (sku != null) {
            sku = sku.trim();
            if (sku.isBlank()) {
                sku = null;
            }
        }
        if (unitLabel != null) {
            unitLabel = unitLabel.trim();
            if (unitLabel.isBlank()) {
                unitLabel = null;
            }
        }
        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }
        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
        }
        if (lineTotal == null) {
            lineTotal = BigDecimal.ZERO;
        }
        unitPrice = unitPrice.setScale(4, RoundingMode.HALF_UP);
        lineTotal = lineTotal.setScale(4, RoundingMode.HALF_UP);
    }
}
