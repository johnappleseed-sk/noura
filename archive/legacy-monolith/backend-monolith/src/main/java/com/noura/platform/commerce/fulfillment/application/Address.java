package com.noura.platform.commerce.fulfillment.application;

/**
 * Shipping address for rate and shipment requests.
 */
public record Address(
        String line1,
        String line2,
        String city,
        String stateCode,
        String postalCode,
        String countryCode
) {
    public Address(String line1, String city, String stateCode, String postalCode, String countryCode) {
        this(line1, null, city, stateCode, postalCode, countryCode);
    }
}
