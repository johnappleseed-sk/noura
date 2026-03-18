package com.noura.checkout.dto.checkout;

import com.noura.checkout.domain.enums.CheckoutPaymentMethod;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Command payload for checkout place-order orchestration.
 *
 * @param storeId optional store/location identifier override
 * @param addressId optional shipping address identifier override
 * @param couponCode optional coupon code placeholder for totals extension
 * @param paymentMethod requested checkout payment method
 * @param paymentProvider requested payment provider, defaulting to sandbox/mock when blank
 * @param paymentProviderReference optional provider-side reference echoed into payment metadata
 * @param paymentAutoCapture when true, payment confirmation uses capture semantics
 * @param idempotencyKey optional idempotency key (header value takes precedence when provided)
 */
public record CheckoutPlaceOrderRequest(
        UUID storeId,
        UUID addressId,
        @Size(max = 80) String couponCode,
        CheckoutPaymentMethod paymentMethod,
        @Size(max = 64) String paymentProvider,
        @Size(max = 128) String paymentProviderReference,
        Boolean paymentAutoCapture,
        @Size(max = 128) String idempotencyKey
) {
}
