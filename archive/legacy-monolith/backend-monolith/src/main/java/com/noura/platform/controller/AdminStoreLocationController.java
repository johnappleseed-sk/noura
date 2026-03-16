package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.location.StoreLocationDto;
import com.noura.platform.dto.location.StoreLocationRequest;
import com.noura.platform.service.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminStoreLocationController {

    private final StoreService storeService;

    @GetMapping("${app.api.version-prefix:/api/v1}/admin/stores/{storeId}/location")
    public ApiResponse<StoreLocationDto> get(@PathVariable UUID storeId, HttpServletRequest http) {
        return ApiResponse.ok("Store location", storeService.getStoreLocation(storeId), http.getRequestURI());
    }

    @PutMapping("${app.api.version-prefix:/api/v1}/admin/stores/{storeId}/location")
    public ApiResponse<StoreLocationDto> update(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreLocationRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Store location updated", storeService.updateStoreLocation(storeId, request), http.getRequestURI());
    }
}
