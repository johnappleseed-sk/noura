package com.noura.platform.location.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal GeoJSON parsing for service-area polygons.
 *
 * <p>Supports Polygon/MultiPolygon and Feature wrappers. Coordinates are expected in [lng, lat] order.</p>
 */
public final class GeoJsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private GeoJsonUtils() {
    }

    public static List<GeoUtils.Point> extractOuterRing(String geoJson) {
        if (geoJson == null || geoJson.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            JsonNode geometry = unwrapGeometry(root);
            if (geometry == null) return Collections.emptyList();

            String type = text(geometry.get("type"));
            JsonNode coordinates = geometry.get("coordinates");
            if (type == null || coordinates == null || coordinates.isNull()) return Collections.emptyList();

            JsonNode ring = switch (type) {
                case "Polygon" -> coordinates.isArray() && coordinates.size() > 0 ? coordinates.get(0) : null;
                case "MultiPolygon" -> coordinates.isArray() && coordinates.size() > 0
                        && coordinates.get(0).isArray() && coordinates.get(0).size() > 0 ? coordinates.get(0).get(0) : null;
                default -> null;
            };
            if (ring == null || !ring.isArray()) return Collections.emptyList();

            List<GeoUtils.Point> points = new ArrayList<>();
            for (JsonNode pair : ring) {
                if (!pair.isArray() || pair.size() < 2) continue;
                double lng = pair.get(0).asDouble();
                double lat = pair.get(1).asDouble();
                points.add(new GeoUtils.Point(lat, lng));
            }
            return points;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static JsonNode unwrapGeometry(JsonNode root) {
        if (root == null || root.isNull()) return null;
        String type = text(root.get("type"));
        if ("Feature".equals(type)) {
            return root.get("geometry");
        }
        if ("FeatureCollection".equals(type)) {
            JsonNode features = root.get("features");
            if (features != null && features.isArray() && features.size() > 0) {
                return unwrapGeometry(features.get(0));
            }
            return null;
        }
        return root;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }
}

