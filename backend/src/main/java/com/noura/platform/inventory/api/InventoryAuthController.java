package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.inventory.dto.auth.InventoryAuthResponse;
import com.noura.platform.inventory.dto.auth.InventoryCurrentUserResponse;
import com.noura.platform.inventory.dto.auth.InventoryLoginRequest;
import com.noura.platform.inventory.dto.auth.InventoryRegisterRequest;
import com.noura.platform.inventory.service.InventoryAuthService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/auth")
public class InventoryAuthController {

    private final InventoryAuthService inventoryAuthService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<InventoryAuthResponse>> register(@Valid @RequestBody InventoryRegisterRequest request,
                                                                       HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Inventory user registered", inventoryAuthService.register(request), http.getRequestURI()));
    }

    @PostMapping("/login")
    public ApiResponse<InventoryAuthResponse> login(@Valid @RequestBody InventoryLoginRequest request,
                                                    HttpServletRequest http) {
        return ApiResponse.ok("Inventory user authenticated", inventoryAuthService.login(request), http.getRequestURI());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<InventoryCurrentUserResponse> currentUser(HttpServletRequest http) {
        return ApiResponse.ok("Authenticated inventory user", inventoryAuthService.currentUser(), http.getRequestURI());
    }
}
