package com.noura.platform.dto.location;

import com.noura.platform.domain.enums.LocationSource;
import com.noura.platform.domain.enums.PhotoPrivacyLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PhotoLocationMetadataDto(
        UUID id,
        UUID mediaId,
        UUID ownerId,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant capturedAt,
        LocationSource source,
        Integer accuracyMeters,
        String addressSnapshot,
        PhotoPrivacyLevel privacyLevel,
        boolean visibleToAdmin,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {
}

