package com.noura.platform.commerce.payments.application;

import java.math.BigDecimal;

/**
 * Abstract interface for payment gateway integrations.
 * Implement this interface for each payment provider (Stripe, PayPal, etc.).
 */
public interface PaymentGateway {

    /**
     * Get the provider identifier (e.g., "stripe", "paypal", "square").
     */
    String getProviderId();

    /**
     * Check if this gateway is enabled and configured.
     */
    boolean isEnabled();

    /**
     * Create a payment intent/authorization for the given amount.
     *
     * @param request Payment creation request
     * @return Result containing provider reference and status
     */
    PaymentResult createPayment(CreatePaymentRequest request);

    /**
     * Capture a previously authorized payment.
     *
     * @param providerReference The provider's reference/transaction ID
     * @param amount Amount to capture (may be less than authorized)
     * @return Result of the capture operation
     */
    PaymentResult capturePayment(String providerReference, BigDecimal amount);

    /**
     * Refund a captured payment.
     *
     * @param providerReference The provider's reference/transaction ID
     * @param amount Amount to refund
     * @param reason Optional reason for the refund
     * @return Result of the refund operation
     */
    PaymentResult refundPayment(String providerReference, BigDecimal amount, String reason);

    /**
     * Cancel/void an authorized but uncaptured payment.
     *
     * @param providerReference The provider's reference/transaction ID
     * @return Result of the void operation
     */
    PaymentResult voidPayment(String providerReference);

    /**
     * Get the current status of a payment.
     *
     * @param providerReference The provider's reference/transaction ID
     * @return Current payment status
     */
    PaymentStatus getPaymentStatus(String providerReference);

    // ===============================
    // Request/Response Records
    // ===============================

    record CreatePaymentRequest(
            BigDecimal amount,
            String currencyCode,
            String orderId,
            String customerEmail,
            String description,
            String returnUrl,
            String cancelUrl
    ) {}

    record PaymentResult(
            boolean success,
            String providerReference,
            PaymentStatus status,
            String errorCode,
            String errorMessage,
            String rawResponse
    ) {
        public static PaymentResult success(String providerReference, PaymentStatus status, String rawResponse) {
            return new PaymentResult(true, providerReference, status, null, null, rawResponse);
        }

        public static PaymentResult failure(String errorCode, String errorMessage) {
            return new PaymentResult(false, null, PaymentStatus.FAILED, errorCode, errorMessage, null);
        }
    }

    enum PaymentStatus {
        PENDING,
        REQUIRES_ACTION,
        AUTHORIZED,
        CAPTURED,
        PARTIALLY_REFUNDED,
        REFUNDED,
        VOIDED,
        FAILED,
        EXPIRED,
        UNKNOWN
    }
}
