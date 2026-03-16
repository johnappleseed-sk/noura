package com.noura.platform.common.web;

import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;

import java.util.LinkedHashMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record SystemHealthStatusResponse(
        String check,
        String service,
        String status,
        List<String> activeProfiles,
        Map<String, Object> details,
        Instant timestamp
) {

    public static SystemHealthStatusResponse from(
            String check,
            String service,
            HealthComponent health,
            List<String> activeProfiles,
            Instant timestamp
    ) {
        String status = health == null ? "DOWN" : health.getStatus().getCode();
        String normalizedService = service == null || service.isBlank() ? "noura-platform" : service;
        Map<String, Object> details = extractDetails(health);
        return new SystemHealthStatusResponse(
                check,
                normalizedService,
                status,
                activeProfiles,
                details,
                timestamp
        );
    }

    private static Map<String, Object> extractDetails(HealthComponent health) {
        if (health == null) {
            return Map.of();
        }
        if (health instanceof Health directHealth) {
            return directHealth.getDetails();
        }
        if (health instanceof CompositeHealth compositeHealth) {
            return compositeHealth.getComponents().entrySet().stream().collect(
                    Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> transformComponent(entry.getValue())
                    )
            );
        }
        return new LinkedHashMap<>();
    }

    private static Object transformComponent(HealthComponent component) {
        if (component == null) {
            return Map.of("status", "UNKNOWN");
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("status", component.getStatus().getCode());
        value.put("details", extractDetails(component));
        return value;
    }
}
