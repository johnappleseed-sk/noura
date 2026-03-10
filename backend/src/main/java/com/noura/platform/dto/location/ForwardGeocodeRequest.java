package com.noura.platform.dto.location;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ForwardGeocodeRequest(
        @NotBlank String query,
        @Min(1) @Max(10) Integer limit,
        String countryCodes,
        String locale
) {
}

