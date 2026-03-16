package com.noura.platform.dto.location;

import com.noura.platform.domain.enums.LocationSource;
import com.noura.platform.domain.enums.PhotoPrivacyLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.Instant;

public record PhotoLocationMetadataRequest(
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        Instant capturedAt,
        LocationSource source,
        Integer accuracyMeters,
        PhotoPrivacyLevel privacyLevel,
        boolean visibleToAdmin,
        boolean reverseGeocode
) {
}

