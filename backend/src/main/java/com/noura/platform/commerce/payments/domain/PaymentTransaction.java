package com.noura.platform.commerce.payments.domain;

import com.noura.platform.commerce.orders.domain.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "customer_order_payment")
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(length = 64)
    private String provider;

    @Column(name = "payment_method", nullable = false, length = 64)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Column(name = "provider_reference", length = 128)
    private String providerReference;

    @Column(name = "failure_reason", length = 400)
    private String failureReason;

    @Column(name = "raw_response", length = 2048)
    private String rawResponse;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        if (currencyCode == null || currencyCode.isBlank()) {
            currencyCode = "USD";
        } else {
            currencyCode = currencyCode.trim().toUpperCase();
        }
        if (paymentMethod != null) {
            paymentMethod = paymentMethod.trim();
        }
        if (provider != null && provider.isBlank()) {
            provider = null;
        }
        if (providerReference != null && providerReference.isBlank()) {
            providerReference = null;
        }
        if (failureReason != null && failureReason.isBlank()) {
            failureReason = null;
        }
        if (rawResponse != null && rawResponse.isBlank()) {
            rawResponse = null;
        }
        if (status == null) {
            status = PaymentTransactionStatus.PENDING;
        }
    }
}
