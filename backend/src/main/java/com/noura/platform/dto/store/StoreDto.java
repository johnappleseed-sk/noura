package com.noura.platform.dto.store;

import com.noura.platform.domain.enums.StoreServiceType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record StoreDto(
        UUID id,
        String name,
        String addressLine1,
        String city,
        String state,
        String zipCode,
        String country,
        String region,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer serviceRadiusMeters,
        LocalTime openTime,
        LocalTime closeTime,
        boolean active,
        Set<StoreServiceType> services,
        BigDecimal shippingFee,
        BigDecimal freeShippingThreshold,
        double distanceKm,
        boolean openNow
) {
}
