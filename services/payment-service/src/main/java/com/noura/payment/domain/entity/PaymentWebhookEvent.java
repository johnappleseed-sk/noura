package com.noura.payment.domain.entity;

import com.noura.payment.domain.enums.PaymentWebhookProcessingStatus;
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

import java.time.Instant;
import java.util.Locale;

/**
 * Persisted webhook delivery used for idempotency and auditability.
 */
@Getter
@Setter
@Entity
@Table(name = "payment_webhook_events")
public class PaymentWebhookEvent extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransaction paymentTransaction;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "provider_event_id", nullable = false, length = 128)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "payment_reference", length = 64)
    private String paymentReference;

    @Column(name = "provider_transaction_id", length = 128)
    private String providerTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    private PaymentWebhookProcessingStatus processingStatus = PaymentWebhookProcessingStatus.RECEIVED;

    @Column(name = "signature_verified", nullable = false)
    private boolean signatureVerified;

    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    /**
     * Normalizes webhook event fields before persistence.
     */
    @PrePersist
    @PreUpdate
    protected void normalize() {
        providerCode = normalizeProviderCode(providerCode);
        providerEventId = trimToNull(providerEventId);
        eventType = trimToNull(eventType);
        paymentReference = trimToNull(paymentReference);
        providerTransactionId = trimToNull(providerTransactionId);
        payloadJson = trimToNull(payloadJson);
        failureReason = trimToNull(failureReason);

        if (processingStatus == null) {
            processingStatus = PaymentWebhookProcessingStatus.RECEIVED;
        }
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
        if (processingStatus == PaymentWebhookProcessingStatus.PROCESSED
                || processingStatus == PaymentWebhookProcessingStatus.FAILED
                || processingStatus == PaymentWebhookProcessingStatus.IGNORED) {
            if (processedAt == null) {
                processedAt = Instant.now();
            }
        } else {
            processedAt = null;
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
     * Normalizes provider code to lowercase.
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
}
