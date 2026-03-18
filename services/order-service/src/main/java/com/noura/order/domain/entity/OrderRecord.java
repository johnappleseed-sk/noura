package com.noura.order.domain.entity;

import com.noura.order.domain.enums.OrderStatus;
import com.noura.order.domain.enums.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order aggregate root that stores immutable checkout snapshots and lifecycle state.
 */
@Getter
@Setter
@Entity
@Table(name = "orders")
public class OrderRecord extends AuditableEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber;

    @Column(name = "customer_ref", nullable = false, length = 180)
    private String customerRef;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "address_id")
    private UUID addressId;

    @Column(name = "currency_code", nullable = false, length = 8)
    private String currencyCode;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "shipping_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal shippingAmount;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @Column(name = "coupon_code", length = 80)
    private String couponCode;

    @Column(name = "shipping_address_snapshot_json")
    private String shippingAddressSnapshotJson;

    @Column(name = "billing_address_snapshot_json")
    private String billingAddressSnapshotJson;

    @Column(name = "checkout_snapshot_json")
    private String checkoutSnapshotJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 32)
    private RefundStatus refundStatus = RefundStatus.NONE;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;
}

