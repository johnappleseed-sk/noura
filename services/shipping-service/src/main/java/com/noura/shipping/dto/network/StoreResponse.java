package com.noura.shipping.dto.network;

import com.noura.shipping.domain.enums.StoreServiceType;
import com.noura.shipping.domain.enums.StoreStatus;
import com.noura.shipping.domain.enums.StoreType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Store response aligned with admin-web and legacy store selectors.
 */
public record StoreResponse(
        UUID id,
        String storeCode,
        String name,
        String slug,
        UUID merchantId,
        StoreType type,
        StoreStatus status,
        String countryCode,
        String city,
        String addressLine1,
        String addressLine2,
        String postalCode,
        String contactEmail,
        String contactPhone,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean openNow,
        boolean preferredStore,
        List<StoreServiceType> supportedServices,
        Long distanceMeters,
        Instant createdAt,
        Instant updatedAt
) {
}
