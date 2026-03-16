package com.noura.platform.common.web;

import com.noura.platform.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Tag(name = "System Health", description = "Platform health and lifecycle readiness endpoints.")
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/system")
public class SystemHealthController {

    private final HealthEndpoint healthEndpoint;
    private final Environment environment;

    @GetMapping("/health")
    @Operation(summary = "Platform health", description = "Returns aggregate system health status for basic service checks.")
    public ResponseEntity<ApiResponse<SystemHealthStatusResponse>> health(HttpServletRequest request) {
        return buildResponse(healthEndpoint.health(), "health", request.getRequestURI());
    }

    @GetMapping("/health/readiness")
    @Operation(summary = "Readiness probe", description = "Returns readiness status using actuator readiness group.")
    public ResponseEntity<ApiResponse<SystemHealthStatusResponse>> readinessGroup(HttpServletRequest request) {
        return buildResponse(
                healthEndpoint.healthForPath("readiness"),
                "readiness",
                request.getRequestURI()
        );
    }

    @GetMapping("/readiness")
    @Operation(summary = "Readiness probe", description = "Returns readiness status for orchestration checks.")
    public ResponseEntity<ApiResponse<SystemHealthStatusResponse>> readiness(HttpServletRequest request) {
        return buildResponse(
                healthEndpoint.healthForPath("readiness"),
                "readiness",
                request.getRequestURI()
        );
    }

    @GetMapping("/liveness")
    @Operation(summary = "Liveness probe", description = "Returns liveness status for process health checks.")
    public ResponseEntity<ApiResponse<SystemHealthStatusResponse>> liveness(HttpServletRequest request) {
        return buildResponse(
                healthEndpoint.healthForPath("liveness"),
                "liveness",
                request.getRequestURI()
        );
    }

    @GetMapping("/health/liveness")
    @Operation(summary = "Liveness probe", description = "Returns liveness status using actuator liveness group.")
    public ResponseEntity<ApiResponse<SystemHealthStatusResponse>> livenessGroup(HttpServletRequest request) {
        return buildResponse(
                healthEndpoint.healthForPath("liveness"),
                "liveness",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiResponse<SystemHealthStatusResponse>> buildResponse(
            HealthComponent health,
            String checkType,
            String path
    ) {
        String status = health.getStatus().getCode();
        HttpStatus responseStatus = "UP".equalsIgnoreCase(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        String activeService = environment.getProperty("spring.application.name", "noura-platform");

        SystemHealthStatusResponse payload = SystemHealthStatusResponse.from(
                checkType,
                activeService,
                health,
                Arrays.asList(environment.getActiveProfiles()),
                Instant.now()
        );

        ApiResponse<SystemHealthStatusResponse> response = "UP".equalsIgnoreCase(status)
                ? ApiResponse.ok("System is healthy", payload, path)
                : ApiResponse.fail(
                        "System is not healthy",
                        "SYSTEM_HEALTH_CHECK_FAILED",
                        "At least one health indicator is reporting a non-healthy state",
                        path
                );

        return ResponseEntity.status(responseStatus).body(response);
    }
}
