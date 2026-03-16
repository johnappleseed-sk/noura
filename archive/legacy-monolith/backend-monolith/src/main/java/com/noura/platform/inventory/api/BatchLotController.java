package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.inventory.dto.batch.BatchLotFilter;
import com.noura.platform.inventory.dto.batch.BatchLotResponse;
import com.noura.platform.inventory.service.BatchLotQueryService;
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

import java.time.LocalDate;
import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/batches")
public class BatchLotController {

    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "receivedAt", "expiryDate", "status", "lotNumber");

    private final BatchLotQueryService batchLotQueryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<PageResponse<BatchLotResponse>> listBatches(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate expiringBefore,
            @RequestParam(required = false) LocalDate expiringAfter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "expiryDate") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = InventoryPageRequestFactory.of(page, size, sortBy, direction, ALLOWED_SORTS, "expiryDate");
        Page<BatchLotResponse> result = batchLotQueryService.listBatches(
                new BatchLotFilter(productId, status, expiringBefore, expiringAfter),
                pageable
        );
        return ApiResponse.ok("Batch lots", PageResponse.from(result), http.getRequestURI());
    }

    @GetMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<BatchLotResponse> getBatch(@PathVariable String batchId, HttpServletRequest http) {
        return ApiResponse.ok("Batch lot", batchLotQueryService.getBatch(batchId), http.getRequestURI());
    }
}
