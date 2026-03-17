package com.noura.payment.dto.payment;

import com.noura.payment.domain.enums.PaymentStatus;
import com.noura.payment.domain.enums.PaymentWebhookProcessingStatus;

import java.util.UUID;

/**
 * Response returned after webhook ingestion.
 *
 * @param providerCode provider code that handled the webhook
 * @param providerEventId provider delivery/event identifier
 * @param eventType normalized event type
 * @param processingStatus webhook processing result
 * @param duplicate whether the delivery was deduplicated
 * @param signatureVerified whether webhook signature verification succeeded
 * @param paymentId linked payment identifier when a payment was resolved
 * @param paymentStatus current linked payment status
 * @param message processing summary
 */
public record PaymentWebhookResponse(
        String providerCode,
        String providerEventId,
        String eventType,
        PaymentWebhookProcessingStatus processingStatus,
        boolean duplicate,
        boolean signatureVerified,
        UUID paymentId,
        PaymentStatus paymentStatus,
        String message
) {
}
