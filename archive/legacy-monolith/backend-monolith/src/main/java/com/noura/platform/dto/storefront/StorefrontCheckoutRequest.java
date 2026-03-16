package com.noura.platform.dto.storefront;

public record StorefrontCheckoutRequest(
        Long shippingAddressId,
        String currency,
        String paymentMethod,
        String paymentProvider,
        String paymentProviderReference
) {
}
