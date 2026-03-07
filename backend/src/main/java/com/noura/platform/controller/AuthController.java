package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.auth.*;
import com.noura.platform.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Executes register.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokensResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registered", authService.register(request), http.getRequestURI()));
    }

    /**
     * Executes login.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/login")
    public ApiResponse<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Logged in", authService.login(request), http.getRequestURI());
    }

    /**
     * Executes refresh.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthTokensResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Token refreshed", authService.refresh(request), http.getRequestURI());
    }

    /**
     * Executes request reset.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/password-reset/request")
    public ApiResponse<Void> requestReset(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest http) {
        authService.requestPasswordReset(request);
        return ApiResponse.ok("If account exists, reset token was generated", null, http.getRequestURI());
    }

    /**
     * Executes confirm reset.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/password-reset/confirm")
    public ApiResponse<Void> confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request, HttpServletRequest http) {
        authService.resetPassword(request);
        return ApiResponse.ok("Password updated", null, http.getRequestURI());
    }
}
