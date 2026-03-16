package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.inventory.InventoryAdjustRequest;
import com.noura.platform.dto.inventory.InventoryLevelDto;
import com.noura.platform.dto.inventory.InventorySummaryDto;
import com.noura.platform.service.StoreInventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/stores/{storeId}/inventory")
public class StoreInventoryController {

    private final StoreInventoryService storeInventoryService;

    @GetMapping("/variants/{variantId}")
    public ApiResponse<InventorySummaryDto> stock(
            @PathVariable UUID storeId,
            @PathVariable UUID variantId,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Store inventory summary", storeInventoryService.stock(storeId, variantId), http.getRequestURI());
    }

    @PostMapping("/adjust")
    public ApiResponse<InventoryLevelDto> adjust(
            @PathVariable UUID storeId,
            @Valid @RequestBody InventoryAdjustRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Store inventory adjusted", storeInventoryService.adjust(storeId, request), http.getRequestURI());
    }
}

