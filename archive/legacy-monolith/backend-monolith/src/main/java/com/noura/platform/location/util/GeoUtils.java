package com.noura.platform.location.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Small geospatial helpers (no external GIS dependency).
 *
 * <p>These are used for deterministic, testable business rules such as radius and polygon inclusion.</p>
 */
public final class GeoUtils {
    private static final double EARTH_RADIUS_METERS = 6_371_000D;

    private GeoUtils() {
    }

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    public static long haversineMeters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return 0L;
        return Math.round(haversineMeters(lat1.doubleValue(), lng1.doubleValue(), lat2.doubleValue(), lng2.doubleValue()));
    }

    /**
     * Ray-casting point-in-polygon check.
     *
     * <p>GeoJSON uses [lng, lat] ordering, so the {@code Point} uses explicit fields.</p>
     */
    public static boolean pointInPolygon(double latitude, double longitude, List<Point> polygon) {
        if (polygon == null || polygon.size() < 3) return false;

        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            double xi = polygon.get(i).longitude();
            double yi = polygon.get(i).latitude();
            double xj = polygon.get(j).longitude();
            double yj = polygon.get(j).latitude();

            boolean intersect = ((yi > latitude) != (yj > latitude))
                    && (longitude < (xj - xi) * (latitude - yi) / (yj - yi + 0.0) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    /**
     * Rounds a coordinate to reduce cache-key cardinality while keeping storefront-appropriate precision.
     */
    public static BigDecimal roundCoordinate(BigDecimal value, int decimals) {
        if (value == null) return null;
        return value.setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    public record Point(double latitude, double longitude) {}
}

