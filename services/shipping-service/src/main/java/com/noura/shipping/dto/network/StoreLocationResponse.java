package com.noura.shipping.dto.network;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Store-location detail payload aligned with legacy admin location dialogs.
 */
public record StoreLocationResponse(
        UUID storeId,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressLine1,
        String addressLine2,
        String city,
        String postalCode,
        String countryCode
) {
}
