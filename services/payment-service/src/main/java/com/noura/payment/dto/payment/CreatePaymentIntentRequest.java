package com.noura.payment.dto.payment;

import com.noura.payment.domain.enums.PaymentMethodType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Command payload for creating a payment intent.
 *
 * @param orderId target order identifier
 * @param methodType payment method type
 * @param providerCode provider code, defaulting to mock when blank
 * @param currencyCode optional currency override
 * @param autoCapture when true, provider may capture immediately
 * @param idempotencyKey optional request idempotency key
 * @param metadata optional client metadata stored with the internal payment record
 */
public record CreatePaymentIntentRequest(
        @NotNull UUID orderId,
        @NotNull PaymentMethodType methodType,
        @Size(max = 64) String providerCode,
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currencyCode must be a 3-letter ISO code")
        String currencyCode,
        Boolean autoCapture,
        @Size(max = 128) String idempotencyKey,
        Map<String, Object> metadata
) {

    /**
     * Resolves provider code with fallback.
     *
     * @return resolved provider code
     */
    public String resolvedProviderCode() {
        if (providerCode == null || providerCode.isBlank()) {
            return "mock";
        }
        return providerCode.trim();
    }

    /**
     * Resolves auto-capture preference with a safe default.
     *
     * @return resolved auto-capture value
     */
    public boolean resolvedAutoCapture() {
        return Boolean.TRUE.equals(autoCapture);
    }
}
