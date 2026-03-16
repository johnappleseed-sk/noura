package com.noura.platform.commerce.fulfillment.application;

/**
 * Contact with address for shipping.
 */
public record ContactAddress(
        String name,
        String phone,
        String email,
        Address address
) {
}
