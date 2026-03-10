package com.noura.platform.dto.location;

import java.math.BigDecimal;

public record GeocodeResultDto(
        BigDecimal latitude,
        BigDecimal longitude,
        String formattedAddress,
        String country,
        String region,
        String city,
        String district,
        String postalCode,
        String placeId
) {
}

