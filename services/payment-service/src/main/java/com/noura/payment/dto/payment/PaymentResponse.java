package com.noura.payment.dto.payment;

import com.noura.payment.domain.enums.PaymentAuthorizationStatus;
import com.noura.payment.domain.enums.PaymentCaptureStatus;
import com.noura.payment.domain.enums.PaymentMethodType;
import com.noura.payment.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Payment transaction response DTO.
 *
 * @param id payment identifier
 * @param orderId order identifier
 * @param customerRef customer reference
 * @param paymentReference payment reference
 * @param methodType payment method type
 * @param status payment status
 * @param authorizationStatus authorization sub-state
 * @param captureStatus capture sub-state
 * @param providerCode provider code
 * @param providerTransactionId provider transaction identifier
 * @param idempotencyKey idempotency key used for intent creation
 * @param amount amount
 * @param authorizedAmount authorized amount snapshot
 * @param capturedAmount captured amount snapshot
 * @param refundedAmount refunded amount snapshot
 * @param currencyCode currency code
 * @param autoCapture auto-capture preference
 * @param requestedAt requested timestamp
 * @param confirmedAt confirmed timestamp
 * @param authorizedAt authorized timestamp
 * @param capturedAt captured timestamp
 * @param completedAt completed timestamp
 * @param lastWebhookReceivedAt last webhook receipt timestamp
 * @param lastWebhookProcessedAt last webhook processed timestamp
 * @param failureReason failure reason
 * @param metadata parsed metadata payload
 * @param createdAt created timestamp
 * @param updatedAt updated timestamp
 */
public record PaymentResponse(
        UUID id,
        UUID orderId,
        String customerRef,
        String paymentReference,
        PaymentMethodType methodType,
        PaymentStatus status,
        PaymentAuthorizationStatus authorizationStatus,
        PaymentCaptureStatus captureStatus,
        String providerCode,
        String providerTransactionId,
        String idempotencyKey,
        BigDecimal amount,
        BigDecimal authorizedAmount,
        BigDecimal capturedAmount,
        BigDecimal refundedAmount,
        String currencyCode,
        boolean autoCapture,
        Instant requestedAt,
        Instant confirmedAt,
        Instant authorizedAt,
        Instant capturedAt,
        Instant completedAt,
        Instant lastWebhookReceivedAt,
        Instant lastWebhookProcessedAt,
        String failureReason,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {
}
