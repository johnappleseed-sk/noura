package com.noura.shipping.dto.network;

import com.noura.shipping.domain.enums.ServiceAreaStatus;
import com.noura.shipping.domain.enums.ServiceAreaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service-area create/update payload aligned with admin-web.
 */
public record ServiceAreaRequest(
        @NotBlank String name,
        @NotNull ServiceAreaType type,
        ServiceAreaStatus status,
        BigDecimal centerLatitude,
        BigDecimal centerLongitude,
        Integer radiusMeters,
        String polygonGeoJson,
        String rulesJson,
        List<UUID> storeIds
) {
}
