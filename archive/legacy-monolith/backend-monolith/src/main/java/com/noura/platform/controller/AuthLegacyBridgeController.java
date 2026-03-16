package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.auth.AuthTokensResponse;
import com.noura.platform.dto.auth.LoginRequest;
import com.noura.platform.dto.user.UserProfileDto;
import com.noura.platform.service.UnifiedAuthService;
import com.noura.platform.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy compatibility controller that keeps `/api/auth/*` behavior aligned with canonical `/api/v1/auth/*` endpoints.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthLegacyBridgeController {

    private final UnifiedAuthService authService;
    private final UserAccountService userAccountService;

    @PostMapping("/login")
    public ApiResponse<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Logged in", authService.login(request), http.getRequestURI());
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileDto> me(HttpServletRequest http) {
        return ApiResponse.ok("Current user", userAccountService.getMyProfile(), http.getRequestURI());
    }
}
