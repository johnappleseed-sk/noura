package com.noura.platform.dto.location;

import java.util.UUID;

public record LocationResolveDto(
        UUID userLocationId,
        GeocodeResultDto geocode,
        ServiceEligibilityDto eligibility
) {
}

