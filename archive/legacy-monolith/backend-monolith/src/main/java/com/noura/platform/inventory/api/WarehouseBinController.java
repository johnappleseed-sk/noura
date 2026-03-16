package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinFilter;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinRequest;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinResponse;
import com.noura.platform.inventory.service.WarehouseBinService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/bins")
public class WarehouseBinController {

    private static final Set<String> BIN_SORTS = Set.of("binCode", "zoneCode", "pickSequence", "createdAt", "updatedAt");

    private final WarehouseBinService warehouseBinService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<PageResponse<WarehouseBinResponse>> listBins(
            @RequestParam(required = false) String warehouseId,
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
        return ApiResponse.ok("Bins", PageResponse.from(response), http.getRequestURI());
    }

    @GetMapping("/{binId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<WarehouseBinResponse> getBin(@PathVariable String binId, HttpServletRequest http) {
        return ApiResponse.ok("Bin", warehouseBinService.getBin(binId), http.getRequestURI());
    }

    @PutMapping("/{binId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ApiResponse<WarehouseBinResponse> updateBin(
            @PathVariable String binId,
            @Valid @RequestBody WarehouseBinRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Bin updated", warehouseBinService.updateBin(binId, request), http.getRequestURI());
    }

    @DeleteMapping("/{binId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ApiResponse<Void> deleteBin(@PathVariable String binId, HttpServletRequest http) {
        warehouseBinService.deleteBin(binId);
        return ApiResponse.ok("Bin deleted", null, http.getRequestURI());
    }
}
