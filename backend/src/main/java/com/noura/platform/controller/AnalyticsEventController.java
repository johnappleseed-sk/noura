package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.analytics.AnalyticsEventDto;
import com.noura.platform.dto.analytics.AnalyticsEventRequest;
import com.noura.platform.dto.analytics.AnalyticsOverviewDto;
import com.noura.platform.dto.analytics.RailPerformanceReportDto;
import com.noura.platform.service.AnalyticsEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class AnalyticsEventController {

    private final AnalyticsEventService analyticsEventService;

    @PostMapping("${app.api.version-prefix:/api/v1}/analytics/events")
    public ResponseEntity<ApiResponse<AnalyticsEventDto>> trackEvent(
            @Valid @RequestBody AnalyticsEventRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Analytics event tracked", analyticsEventService.track(request), http.getRequestURI()));
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/analytics/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnalyticsOverviewDto> overview(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Analytics overview", analyticsEventService.overview(from, to), http.getRequestURI());
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/analytics/rails")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RailPerformanceReportDto> railPerformance(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false, defaultValue = "home-") String listNamePrefix,
            @RequestParam(required = false, defaultValue = "/") String pagePath,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Rail performance",
                analyticsEventService.railPerformance(from, to, listNamePrefix, pagePath),
                http.getRequestURI()
        );
    }
}
