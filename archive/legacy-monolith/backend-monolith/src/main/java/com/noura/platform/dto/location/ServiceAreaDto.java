package com.noura.platform.dto.location;

import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ServiceAreaDto(
        UUID id,
        String name,
        ServiceAreaType type,
        ServiceAreaStatus status,
        BigDecimal centerLatitude,
        BigDecimal centerLongitude,
        Integer radiusMeters,
        String polygonGeoJson,
        String rulesJson,
        List<UUID> storeIds,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {
}

