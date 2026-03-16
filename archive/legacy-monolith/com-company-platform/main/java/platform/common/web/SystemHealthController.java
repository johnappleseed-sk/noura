package com.company.platform.common.web;

import com.company.platform.common.api.ApiResponse;
import com.company.platform.common.service.PlatformHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${company.platform.api.version-prefix:/api/v1}/system")
@Tag(name = "System", description = "Platform system endpoints.")
public class SystemHealthController {

    private final PlatformHealthService platformHealthService;

    @GetMapping("/health")
    @Operation(summary = "Platform health check", description = "Returns a lightweight health payload for the platform bootstrap layer.")
    public ResponseEntity<ApiResponse<HealthStatusResponse>> health() {
        return ResponseEntity.ok(ApiResponse.ok("Platform is healthy", platformHealthService.getHealthStatus()));
    }
}
