package com.noura.platform.dto.location;

import com.noura.platform.domain.enums.StoreServiceType;

import java.util.UUID;

public record ServiceEligibilityDto(
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

