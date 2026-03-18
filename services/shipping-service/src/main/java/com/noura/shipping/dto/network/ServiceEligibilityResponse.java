package com.noura.shipping.dto.network;

import com.noura.shipping.domain.enums.StoreServiceType;

import java.util.UUID;

/**
 * Service-area validation response aligned with admin-web.
 */
public record ServiceEligibilityResponse(
        boolean serviceAvailable,
        StoreServiceType serviceType,
        UUID matchedServiceAreaId,
        UUID matchedStoreId,
        Long distanceMeters,
        boolean insideServiceArea,
        boolean storeOpenNow,
        String eligibilityReason
) {
}
