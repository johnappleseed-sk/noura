package com.noura.platform.dto.location;

import com.noura.platform.domain.enums.LocationSource;
import com.noura.platform.domain.enums.StoreServiceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LocationResolveRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        Integer accuracyMeters,
        LocationSource source,
        boolean consentGiven,
        String purpose,
        boolean persist,
        StoreServiceType serviceType
) {
}

