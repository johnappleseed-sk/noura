package com.noura.shipping.dto.network;

import com.noura.shipping.domain.enums.ServiceAreaStatus;
import com.noura.shipping.domain.enums.ServiceAreaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service-area response aligned with admin-web.
 */
public record ServiceAreaResponse(
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
        Instant updatedAt
) {
}
