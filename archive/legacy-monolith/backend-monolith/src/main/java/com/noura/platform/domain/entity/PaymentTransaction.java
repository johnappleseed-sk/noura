package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.PaymentMethodType;
import com.noura.platform.domain.enums.PaymentStatus;
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
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_id", insertable = false, updatable = false)
    private UUID orderId;

    @Column(name = "payment_reference", nullable = false, length = 64)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false, length = 40)
    private PaymentMethodType methodType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "provider_transaction_id", length = 128)
    private String providerTransactionId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @PrePersist
    @PreUpdate
    void normalize() {
        paymentReference = trim(paymentReference);
        providerCode = normalizeProviderCode(providerCode);
        providerTransactionId = trim(providerTransactionId);
        currencyCode = normalizeCurrencyCode(currencyCode);
        failureReason = trim(failureReason);
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
        if (status == PaymentStatus.PENDING) {
            completedAt = null;
        } else if (completedAt == null) {
            completedAt = Instant.now();
        }
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeCurrencyCode(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            return "USD";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeProviderCode(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
