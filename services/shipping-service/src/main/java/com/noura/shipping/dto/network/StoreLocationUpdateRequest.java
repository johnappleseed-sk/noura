package com.noura.shipping.dto.network;

import java.math.BigDecimal;

/**
 * Store-location update payload aligned with legacy admin location dialogs.
 */
public record StoreLocationUpdateRequest(
        BigDecimal latitude,
        BigDecimal longitude,
        String addressLine1,
        String addressLine2,
        String city,
        String postalCode,
        String countryCode
) {
}
