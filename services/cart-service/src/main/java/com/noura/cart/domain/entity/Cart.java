package com.noura.cart.domain.entity;

import com.noura.cart.domain.enums.CartOwnerType;
import com.noura.cart.domain.enums.CartStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Cart aggregate root persisted by the Cart Service.
 */
@Getter
@Setter
@Entity
@Table(name = "carts")
public class Cart extends AuditableEntity {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private CartOwnerType ownerType;

    @Column(name = "customer_id", length = 180)
    private String customerId;

    @Column(name = "guest_token", length = 180)
    private String guestToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CartStatus status = CartStatus.ACTIVE;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "address_id")
    private UUID addressId;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Column(name = "subtotal", nullable = false, precision = 18, scale = 4)
    private BigDecimal subtotal = ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal discountAmount = ZERO;

    @Column(name = "shipping_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal shippingAmount = ZERO;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount = ZERO;

    @Column(name = "coupon_code", length = 120)
    private String couponCode;

    @Column(name = "merged_into_cart_id")
    private UUID mergedIntoCartId;
}
