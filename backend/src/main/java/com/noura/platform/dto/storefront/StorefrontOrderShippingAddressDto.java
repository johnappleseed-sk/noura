package com.noura.platform.dto.storefront;

public record StorefrontOrderShippingAddressDto(
        String recipientName,
        String phone,
        String line1,
        String line2,
        String district,
        String city,
        String stateProvince,
        String postalCode,
        String countryCode
) {
}
