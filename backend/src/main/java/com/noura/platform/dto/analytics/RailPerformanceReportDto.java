package com.noura.platform.dto.analytics;

import java.time.Instant;
import java.util.List;

public record RailPerformanceReportDto(
        Instant from,
        Instant to,
        String listNamePrefix,
        String pagePath,
        List<RailPerformanceDto> rails
) {
}

