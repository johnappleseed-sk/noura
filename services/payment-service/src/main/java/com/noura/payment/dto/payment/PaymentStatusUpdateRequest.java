package com.noura.payment.dto.payment;

import com.noura.payment.domain.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Command payload for internal status updates or webhook updates.
 *
 * @param paymentId payment identifier
 * @param paymentReference payment reference
 * @param providerCode provider code
 * @param status target status
 * @param providerTransactionId optional provider transaction ID
 * @param providerEventId optional provider event identifier for idempotent status updates
 * @param eventType optional event type label
 * @param failureReason optional failure reason
 * @param metadata optional metadata merged into stored payment metadata
 */
public record PaymentStatusUpdateRequest(
        UUID paymentId,
        @Size(max = 64) String paymentReference,
        @Size(max = 64) String providerCode,
        @NotNull PaymentStatus status,
        @Size(max = 128) String providerTransactionId,
        @Size(max = 128) String providerEventId,
        @Size(max = 80) String eventType,
        @Size(max = 500) String failureReason,
        Map<String, Object> metadata
) {
}
