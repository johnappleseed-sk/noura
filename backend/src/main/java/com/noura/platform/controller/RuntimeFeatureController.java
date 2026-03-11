package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.runtime.RuntimeFeaturesDto;
import com.noura.platform.service.RuntimeFeatureService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/runtime")
public class RuntimeFeatureController {

    private final RuntimeFeatureService runtimeFeatureService;

    /**
     * Executes features.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/features")
    public ApiResponse<RuntimeFeaturesDto> features(HttpServletRequest http) {
        return ApiResponse.ok("Runtime features", runtimeFeatureService.describeRuntimeFeatures(), http.getRequestURI());
    }
}

