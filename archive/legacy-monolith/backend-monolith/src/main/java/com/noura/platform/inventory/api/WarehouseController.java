package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinFilter;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinRequest;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinResponse;
import com.noura.platform.inventory.dto.warehouse.WarehouseFilter;
import com.noura.platform.inventory.dto.warehouse.WarehouseRequest;
import com.noura.platform.inventory.dto.warehouse.WarehouseResponse;
import com.noura.platform.inventory.service.WarehouseBinService;
import com.noura.platform.inventory.service.WarehouseService;
import com.noura.platform.inventory.support.InventoryPageRequestFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/warehouses")
public class WarehouseController {

    private static final Set<String> WAREHOUSE_SORTS = Set.of("name", "warehouseCode", "city", "createdAt", "updatedAt");
    private static final Set<String> BIN_SORTS = Set.of("binCode", "zoneCode", "pickSequence", "createdAt", "updatedAt");

    private final WarehouseService warehouseService;
    private final WarehouseBinService warehouseBinService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<PageResponse<WarehouseResponse>> listWarehouses(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = InventoryPageRequestFactory.of(page, size, sortBy, direction, WAREHOUSE_SORTS, "name");
        Page<WarehouseResponse> response = warehouseService.listWarehouses(new WarehouseFilter(query, active), pageable);
        return ApiResponse.ok("Warehouses", PageResponse.from(response), http.getRequestURI());
    }

    @GetMapping("/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<WarehouseResponse> getWarehouse(@PathVariable String warehouseId, HttpServletRequest http) {
        return ApiResponse.ok("Warehouse", warehouseService.getWarehouse(warehouseId), http.getRequestURI());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<WarehouseResponse>> createWarehouse(
            @Valid @RequestBody WarehouseRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Warehouse created", warehouseService.createWarehouse(request), http.getRequestURI()));
    }

    @PutMapping("/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ApiResponse<WarehouseResponse> updateWarehouse(
            @PathVariable String warehouseId,
            @Valid @RequestBody WarehouseRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Warehouse updated", warehouseService.updateWarehouse(warehouseId, request), http.getRequestURI());
    }

    @DeleteMapping("/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ApiResponse<Void> deleteWarehouse(@PathVariable String warehouseId, HttpServletRequest http) {
        warehouseService.deleteWarehouse(warehouseId);
        return ApiResponse.ok("Warehouse deleted", null, http.getRequestURI());
    }

    @GetMapping("/{warehouseId}/bins")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<PageResponse<WarehouseBinResponse>> listWarehouseBins(
            @PathVariable String warehouseId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String zoneCode,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "binCode") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = InventoryPageRequestFactory.of(page, size, sortBy, direction, BIN_SORTS, "binCode");
        Page<WarehouseBinResponse> response = warehouseBinService.listBins(
                new WarehouseBinFilter(warehouseId, query, zoneCode, active),
                pageable
        );
        return ApiResponse.ok("Warehouse bins", PageResponse.from(response), http.getRequestURI());
    }

    @PostMapping("/{warehouseId}/bins")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<WarehouseBinResponse>> createWarehouseBin(
            @PathVariable String warehouseId,
            @Valid @RequestBody WarehouseBinRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Warehouse bin created", warehouseBinService.createBin(warehouseId, request), http.getRequestURI()));
    }
}
