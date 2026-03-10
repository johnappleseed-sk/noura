package com.noura.platform.dto.analytics;

import java.math.BigDecimal;

public record RailPerformanceDto(
        String listName,
        long impressions,
        long clicks,
        long addToCart,
        BigDecimal clickThroughRate,
        BigDecimal clickToCartRate,
        BigDecimal impressionToCartRate
) {
}

