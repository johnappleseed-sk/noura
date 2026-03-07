package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.inventory.*;
import com.noura.platform.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Creates warehouse.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/warehouses")
    public ApiResponse<WarehouseDto> createWarehouse(@Valid @RequestBody WarehouseRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Warehouse created", inventoryService.createWarehouse(request), http.getRequestURI());
    }

    /**
     * Retrieves warehouses.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/warehouses")
    public ApiResponse<List<WarehouseDto>> warehouses(HttpServletRequest http) {
        return ApiResponse.ok("Warehouses", inventoryService.warehouses(), http.getRequestURI());
    }

    /**
     * Retrieves stock.
     *
     * @param variantId The variant id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping({"/{variantId}", "/variants/{variantId}"})
    public ApiResponse<InventorySummaryDto> stock(@PathVariable UUID variantId, HttpServletRequest http) {
        return ApiResponse.ok("Inventory summary", inventoryService.stock(variantId), http.getRequestURI());
    }

    /**
     * Adjusts stock.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/adjust")
    public ApiResponse<InventoryLevelDto> adjust(@Valid @RequestBody InventoryAdjustRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Inventory adjusted", inventoryService.adjust(request), http.getRequestURI());
    }

    /**
     * Reserves stock.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/reserve")
    public ApiResponse<InventoryReservationDto> reserve(
            @Valid @RequestBody InventoryReserveRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Inventory reserved", inventoryService.reserve(request), http.getRequestURI());
    }

    /**
     * Confirms reservation.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/confirm")
    public ApiResponse<InventoryReservationDto> confirm(
            @Valid @RequestBody InventoryReservationActionRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Reservation confirmed", inventoryService.confirm(request), http.getRequestURI());
    }

    /**
     * Releases reservation.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/release")
    public ApiResponse<InventoryReservationDto> release(
            @Valid @RequestBody InventoryReservationActionRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Reservation released", inventoryService.release(request), http.getRequestURI());
    }

    /**
     * Checks availability.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/check")
    public ApiResponse<InventoryCheckResultDto> check(@Valid @RequestBody InventoryCheckRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Inventory availability checked", inventoryService.check(request), http.getRequestURI());
    }
}
