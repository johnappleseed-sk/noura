package com.noura.platform.dto.storefront;

public record StorefrontCustomerAddressRequest(
        String label,
        String recipientName,
        String phone,
        String line1,
        String line2,
        String district,
        String city,
        String stateProvince,
        String postalCode,
        String countryCode,
        boolean defaultShipping,
        boolean defaultBilling
) {
}
