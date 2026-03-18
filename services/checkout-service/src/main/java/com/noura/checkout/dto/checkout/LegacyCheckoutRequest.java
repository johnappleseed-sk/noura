package com.noura.checkout.dto.checkout;

import com.noura.checkout.domain.enums.CheckoutPaymentMethod;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Legacy direct-checkout request payload used by storefront compatibility routes.
 *
 * @param fulfillmentMethod legacy fulfillment method marker (currently informational)
 * @param storeId optional store/location identifier override
 * @param addressId optional shipping address identifier override
 * @param shippingAddressSnapshot legacy shipping snapshot string
 * @param paymentMethod requested checkout payment method
 * @param paymentProvider requested payment provider
 * @param paymentProviderReference optional provider-side reference
 * @param couponCode optional coupon code
 * @param b2bInvoice legacy B2B invoice flag (currently ignored)
 * @param paymentAutoCapture optional capture preference
 * @param idempotencyKey optional idempotency key
 */
public record LegacyCheckoutRequest(
        String fulfillmentMethod,
        UUID storeId,
        UUID addressId,
        String shippingAddressSnapshot,
        CheckoutPaymentMethod paymentMethod,
        @Size(max = 64) String paymentProvider,
        @Size(max = 128) String paymentProviderReference,
        @Size(max = 80) String couponCode,
        Boolean b2bInvoice,
        Boolean paymentAutoCapture,
        @Size(max = 128) String idempotencyKey
) {
}
