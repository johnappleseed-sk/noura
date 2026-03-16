package com.noura.cart.domain.entity;

import com.noura.cart.domain.enums.CartItemValidationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Cart line item snapshot containing quantity, price, and validation state.
 */
@Getter
@Setter
@Entity
@Table(name = "cart_items")
public class CartItem extends AuditableEntity {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @Column(name = "cart_id", insertable = false, updatable = false)
    private UUID cartId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "product_code_snapshot", length = 120)
    private String productCodeSnapshot;

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "sku_snapshot", length = 120)
    private String skuSnapshot;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice = ZERO;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 4)
    private BigDecimal lineTotal = ZERO;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 40)
    private CartItemValidationStatus validationStatus = CartItemValidationStatus.VALID;

    @Column(name = "validation_message", length = 500)
    private String validationMessage;

    @Column(name = "available_quantity", precision = 18, scale = 4)
    private BigDecimal availableQuantity;

    /**
     * Validates line state before first persistence and updates.
     */
    @PrePersist
    @PreUpdate
    protected void validateState() {
        if (quantity < 1) {
            throw new IllegalArgumentException("Cart item quantity must be positive");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Cart item unit price must be non-negative");
        }
        if (lineTotal == null || lineTotal.signum() < 0) {
            throw new IllegalArgumentException("Cart item line total must be non-negative");
        }
    }
}
