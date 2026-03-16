package com.noura.platform.location.util;

import java.math.BigDecimal;

public final class LocationCacheKeys {
    private static final int DEFAULT_DECIMALS = 5;

    private LocationCacheKeys() {
    }

    public static String reverseKey(BigDecimal latitude, BigDecimal longitude) {
        BigDecimal lat = GeoUtils.roundCoordinate(latitude, DEFAULT_DECIMALS);
        BigDecimal lng = GeoUtils.roundCoordinate(longitude, DEFAULT_DECIMALS);
        return (lat == null ? "null" : lat.toPlainString()) + ":" + (lng == null ? "null" : lng.toPlainString());
    }
}

