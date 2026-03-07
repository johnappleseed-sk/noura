package com.noura.platform.commerce.orders.domain;

import com.noura.platform.commerce.customers.domain.CustomerAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "customer_order", indexes = {
        @Index(name = "idx_customer_order_status", columnList = "status"),
        @Index(name = "idx_customer_order_customer", columnList = "customer_account_id"),
        @Index(name = "idx_customer_order_placed_at", columnList = "placed_at")
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_account_id")
    private CustomerAccount customerAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.DRAFT;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Column(name = "customer_email", nullable = false, length = 160)
    private String customerEmail;

    @Column(name = "customer_phone", length = 32)
    private String customerPhone;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "shipping_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippingTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "shipping_recipient_name", length = 120)
    private String shippingRecipientName;

    @Column(name = "shipping_phone", length = 32)
    private String shippingPhone;

    @Column(name = "shipping_line1", length = 255)
    private String shippingLine1;

    @Column(name = "shipping_line2", length = 255)
    private String shippingLine2;

    @Column(name = "shipping_district", length = 120)
    private String shippingDistrict;

    @Column(name = "shipping_city", length = 120)
    private String shippingCity;

    @Column(name = "shipping_state_province", length = 120)
    private String shippingStateProvince;

    @Column(name = "shipping_postal_code", length = 40)
    private String shippingPostalCode;

    @Column(name = "shipping_country_code", length = 2)
    private String shippingCountryCode;

    @Column(name = "placed_at")
    private LocalDateTime placedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

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
        if (orderNumber != null) {
            orderNumber = orderNumber.trim();
        }
        if (currencyCode != null) {
            currencyCode = currencyCode.trim().toUpperCase();
        }
        if (customerEmail != null) {
            customerEmail = customerEmail.trim().toLowerCase();
        }
        if (customerPhone != null) {
            customerPhone = customerPhone.trim();
            if (customerPhone.isEmpty()) {
                customerPhone = null;
            }
        }
        if (status == null) {
            status = OrderStatus.DRAFT;
        }
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }
        if (discountTotal == null) {
            discountTotal = BigDecimal.ZERO;
        }
        if (taxTotal == null) {
            taxTotal = BigDecimal.ZERO;
        }
        if (shippingTotal == null) {
            shippingTotal = BigDecimal.ZERO;
        }
        if (grandTotal == null) {
            grandTotal = BigDecimal.ZERO;
        }
        if (shippingRecipientName != null) {
            shippingRecipientName = shippingRecipientName.trim();
            if (shippingRecipientName.isEmpty()) {
                shippingRecipientName = null;
            }
        }
        if (shippingPhone != null) {
            shippingPhone = shippingPhone.trim();
            if (shippingPhone.isEmpty()) {
                shippingPhone = null;
            }
        }
        if (shippingLine1 != null) {
            shippingLine1 = shippingLine1.trim();
            if (shippingLine1.isEmpty()) {
                shippingLine1 = null;
            }
        }
        if (shippingLine2 != null) {
            shippingLine2 = shippingLine2.trim();
            if (shippingLine2.isEmpty()) {
                shippingLine2 = null;
            }
        }
        if (shippingDistrict != null) {
            shippingDistrict = shippingDistrict.trim();
            if (shippingDistrict.isEmpty()) {
                shippingDistrict = null;
            }
        }
        if (shippingCity != null) {
            shippingCity = shippingCity.trim();
            if (shippingCity.isEmpty()) {
                shippingCity = null;
            }
        }
        if (shippingStateProvince != null) {
            shippingStateProvince = shippingStateProvince.trim();
            if (shippingStateProvince.isEmpty()) {
                shippingStateProvince = null;
            }
        }
        if (shippingPostalCode != null) {
            shippingPostalCode = shippingPostalCode.trim();
            if (shippingPostalCode.isEmpty()) {
                shippingPostalCode = null;
            }
        }
        if (shippingCountryCode != null) {
            shippingCountryCode = shippingCountryCode.trim().toUpperCase();
            if (shippingCountryCode.isEmpty()) {
                shippingCountryCode = null;
            } else if (shippingCountryCode.length() != 2) {
                shippingCountryCode = shippingCountryCode.length() > 2 ? shippingCountryCode.substring(0, 2) : shippingCountryCode;
            }
        }
    }
}
