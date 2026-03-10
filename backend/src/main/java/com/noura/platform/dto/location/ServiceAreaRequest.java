package com.noura.platform.dto.location;

import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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

