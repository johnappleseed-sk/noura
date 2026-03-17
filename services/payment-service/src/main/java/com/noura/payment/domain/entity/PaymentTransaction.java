package com.noura.payment.domain.entity;

import com.noura.payment.domain.enums.PaymentAuthorizationStatus;
import com.noura.payment.domain.enums.PaymentCaptureStatus;
import com.noura.payment.domain.enums.PaymentMethodType;
import com.noura.payment.domain.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Payment aggregate root representing one payment intent/transaction for an order.
 */
@Getter
@Setter
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction extends AuditableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_ref", nullable = false, length = 180)
    private String customerRef;

    @Column(name = "payment_reference", nullable = false, length = 64, unique = true)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false, length = 40)
    private PaymentMethodType methodType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "authorization_status", nullable = false, length = 32)
    private PaymentAuthorizationStatus authorizationStatus = PaymentAuthorizationStatus.NOT_REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "capture_status", nullable = false, length = 32)
    private PaymentCaptureStatus captureStatus = PaymentCaptureStatus.NOT_CAPTURED;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "provider_transaction_id", length = 128)
    private String providerTransactionId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal amount;

    @Column(name = "authorized_amount", precision = 14, scale = 4)
    private BigDecimal authorizedAmount;

    @Column(name = "captured_amount", precision = 14, scale = 4)
    private BigDecimal capturedAmount;

    @Column(name = "refunded_amount", precision = 14, scale = 4)
    private BigDecimal refundedAmount;

    @Column(name = "currency_code", nullable = false, length = 8)
    private String currencyCode;

    @Column(name = "auto_capture", nullable = false)
    private boolean autoCapture;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_webhook_received_at")
    private Instant lastWebhookReceivedAt;

    @Column(name = "last_webhook_processed_at")
    private Instant lastWebhookProcessedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "metadata_json")
    private String metadataJson;

    /**
     * Normalizes mutable fields before insert/update.
     */
    @PrePersist
    @PreUpdate
    protected void normalize() {
        customerRef = trimToNull(customerRef);
        paymentReference = trimToNull(paymentReference);
        providerCode = normalizeProviderCode(providerCode);
        providerTransactionId = trimToNull(providerTransactionId);
        idempotencyKey = trimToNull(idempotencyKey);
        currencyCode = normalizeCurrencyCode(currencyCode);
        failureReason = trimToNull(failureReason);
        metadataJson = trimToNull(metadataJson);

        if (status == null) {
            status = PaymentStatus.CREATED;
        }
        if (authorizationStatus == null) {
            authorizationStatus = PaymentAuthorizationStatus.NOT_REQUESTED;
        }
        if (captureStatus == null) {
            captureStatus = PaymentCaptureStatus.NOT_CAPTURED;
        }
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        if (status == PaymentStatus.CAPTURED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.CANCELED
                || status == PaymentStatus.REFUNDED) {
            if (completedAt == null) {
                completedAt = Instant.now();
            }
        } else {
            completedAt = null;
        }
    }

    /**
     * Trims source text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Normalizes provider code as lowercase.
     *
     * @param value source provider code
     * @return normalized provider code
     */
    private String normalizeProviderCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes currency code as uppercase with default value.
     *
     * @param value source currency code
     * @return normalized currency code
     */
    private String normalizeCurrencyCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "USD";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
