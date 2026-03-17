package com.noura.payment.provider;

import com.noura.payment.domain.enums.PaymentAuthorizationStatus;
import com.noura.payment.domain.enums.PaymentCaptureStatus;
import com.noura.payment.domain.enums.PaymentMethodType;
import com.noura.payment.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Abstraction boundary for provider-specific payment behavior.
 */
public interface PaymentProvider {

    /**
     * Returns the primary provider code handled by this adapter.
     *
     * @return provider code
     */
    String providerCode();

    /**
     * Returns whether this adapter supports a requested provider code.
     *
     * @param requestedProviderCode requested provider code
     * @return {@code true} when supported
     */
    default boolean supports(String requestedProviderCode) {
        if (requestedProviderCode == null || requestedProviderCode.isBlank()) {
            return false;
        }
        return providerCode().equalsIgnoreCase(requestedProviderCode.trim());
    }

    /**
     * Creates a provider-side payment intent/session.
     *
     * @param request provider create request
     * @return normalized provider result
     */
    ProviderOperationResult createPaymentIntent(CreateRequest request);

    /**
     * Performs authorization without capture.
     *
     * @param request provider authorization request
     * @return normalized provider result
     */
    ProviderOperationResult authorizePayment(AuthorizeRequest request);

    /**
     * Performs capture, including provider-specific auth-and-capture shortcuts when supported.
     *
     * @param request provider capture request
     * @return normalized provider result
     */
    ProviderOperationResult capturePayment(CaptureRequest request);

    /**
     * Fetches provider-side payment status.
     *
     * @param request provider status request
     * @return normalized provider result
     */
    ProviderOperationResult fetchPaymentStatus(StatusRequest request);

    /**
     * Parses and validates a webhook payload into normalized payment state.
     *
     * @param request webhook request context
     * @return normalized webhook notification
     */
    ProviderWebhookNotification parseWebhook(WebhookRequest request);

    /**
     * Provider payment-intent creation input.
     */
    record CreateRequest(
            UUID paymentId,
            UUID orderId,
            String orderNumber,
            String paymentReference,
            PaymentMethodType methodType,
            BigDecimal amount,
            String currencyCode,
            boolean autoCapture,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Provider authorization input.
     */
    record AuthorizeRequest(
            UUID paymentId,
            UUID orderId,
            String paymentReference,
            String providerTransactionId,
            BigDecimal amount,
            String currencyCode,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Provider capture input.
     */
    record CaptureRequest(
            UUID paymentId,
            UUID orderId,
            String paymentReference,
            String providerTransactionId,
            BigDecimal amount,
            String currencyCode,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Provider status lookup input.
     */
    record StatusRequest(
            UUID paymentId,
            String paymentReference,
            String providerTransactionId,
            PaymentStatus currentStatus,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Provider webhook parsing input.
     */
    record WebhookRequest(
            String providerCode,
            String payload,
            Map<String, String> headers
    ) {
    }

    /**
     * Provider result normalized to internal payment vocabulary.
     */
    record ProviderOperationResult(
            PaymentStatus status,
            PaymentAuthorizationStatus authorizationStatus,
            PaymentCaptureStatus captureStatus,
            String providerTransactionId,
            String failureReason
    ) {
    }

    /**
     * Normalized provider webhook notification.
     */
    record ProviderWebhookNotification(
            String providerCode,
            String providerEventId,
            String eventType,
            String paymentReference,
            String providerTransactionId,
            PaymentStatus status,
            PaymentAuthorizationStatus authorizationStatus,
            PaymentCaptureStatus captureStatus,
            String failureReason,
            boolean signatureVerified,
            String payloadJson
    ) {
    }
}
