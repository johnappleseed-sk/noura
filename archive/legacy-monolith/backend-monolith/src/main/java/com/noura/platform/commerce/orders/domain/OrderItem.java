package com.noura.platform.commerce.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "customer_order_item", indexes = {
        @Index(name = "idx_customer_order_item_order", columnList = "order_id")
})
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(length = 128)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "unit_label", length = 64)
    private String unitLabel;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (sku != null) {
            sku = sku.trim();
            if (sku.isEmpty()) {
                sku = null;
            }
        }
        if (productName != null) {
            productName = productName.trim();
        }
        if (unitLabel != null) {
            unitLabel = unitLabel.trim();
            if (unitLabel.isEmpty()) {
                unitLabel = null;
            }
        }
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
        }
        if (lineTotal == null) {
            lineTotal = BigDecimal.ZERO;
        }
    }
}
