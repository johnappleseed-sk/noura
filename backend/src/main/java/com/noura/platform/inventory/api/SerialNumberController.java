package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.inventory.dto.serial.SerialNumberFilter;
import com.noura.platform.inventory.dto.serial.SerialNumberResponse;
import com.noura.platform.inventory.service.SerialTrackingService;
import com.noura.platform.inventory.support.InventoryPageRequestFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/serials")
public class SerialNumberController {

    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "updatedAt", "serialNumber", "serialStatus");

    private final SerialTrackingService serialTrackingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<PageResponse<SerialNumberResponse>> listSerials(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String serialStatus,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String binId,
            @RequestParam(required = false) String batchId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = InventoryPageRequestFactory.of(page, size, sortBy, direction, ALLOWED_SORTS, "updatedAt");
        Page<SerialNumberResponse> result = serialTrackingService.listSerials(
                new SerialNumberFilter(query, productId, serialStatus, warehouseId, binId, batchId),
                pageable
        );
        return ApiResponse.ok("Serial numbers", PageResponse.from(result), http.getRequestURI());
    }

    @GetMapping("/{serialId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<SerialNumberResponse> getSerial(@PathVariable String serialId, HttpServletRequest http) {
        return ApiResponse.ok("Serial number", serialTrackingService.getSerial(serialId), http.getRequestURI());
    }
}
