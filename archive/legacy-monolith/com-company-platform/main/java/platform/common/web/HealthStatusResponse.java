package com.company.platform.common.web;

import java.time.Instant;
import java.util.List;

public record HealthStatusResponse(
        String application,
        String environment,
        String status,
        List<String> activeProfiles,
        Instant timestamp
) {
}
