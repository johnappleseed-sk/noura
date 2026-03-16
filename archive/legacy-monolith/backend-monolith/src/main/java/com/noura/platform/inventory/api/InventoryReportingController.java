package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.inventory.dto.report.InventoryTurnoverReportResponse;
import com.noura.platform.inventory.dto.report.LowStockReportItemResponse;
import com.noura.platform.inventory.dto.report.StockValuationReportResponse;
import com.noura.platform.inventory.dto.stock.StockMovementFilter;
import com.noura.platform.inventory.dto.stock.StockMovementResponse;
import com.noura.platform.inventory.service.InventoryReportingService;
import com.noura.platform.inventory.service.StockMovementService;
import com.noura.platform.inventory.support.InventoryPageRequestFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/reports")
public class InventoryReportingController {

    private static final Set<String> MOVEMENT_SORTS = Set.of("createdAt", "processedAt", "movementNumber", "movementType", "movementStatus");

    private final InventoryReportingService inventoryReportingService;
    private final StockMovementService stockMovementService;

    @GetMapping("/stock-valuation")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<StockValuationReportResponse> stockValuation(@RequestParam(required = false) String warehouseId,
                                                                    @RequestParam(required = false) String productId,
                                                                    HttpServletRequest http) {
        return ApiResponse.ok("Stock valuation report", inventoryReportingService.getStockValuationReport(warehouseId, productId), http.getRequestURI());
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<List<LowStockReportItemResponse>> lowStock(@RequestParam(required = false) String warehouseId,
                                                                  HttpServletRequest http) {
        return ApiResponse.ok("Low stock report", inventoryReportingService.getLowStockReport(warehouseId), http.getRequestURI());
    }

    @GetMapping("/turnover")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<InventoryTurnoverReportResponse> turnover(@RequestParam(required = false) Instant dateFrom,
                                                                 @RequestParam(required = false) Instant dateTo,
                                                                 HttpServletRequest http) {
        return ApiResponse.ok("Inventory turnover report", inventoryReportingService.getTurnoverReport(dateFrom, dateTo), http.getRequestURI());
    }

    @GetMapping("/movement-history")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<PageResponse<StockMovementResponse>> movementHistory(
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) String movementStatus,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String referenceQuery,
            @RequestParam(required = false) Instant processedFrom,
            @RequestParam(required = false) Instant processedTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "processedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = InventoryPageRequestFactory.of(page, size, sortBy, direction, MOVEMENT_SORTS, "processedAt");
        Page<StockMovementResponse> result = stockMovementService.listMovements(
                new StockMovementFilter(movementType, movementStatus, warehouseId, productId, referenceQuery, processedFrom, processedTo),
                pageable
        );
        return ApiResponse.ok("Movement history report", PageResponse.from(result), http.getRequestURI());
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ResponseEntity<byte[]> exportCsv(@RequestParam String reportType,
                                            @RequestParam(required = false) String warehouseId,
                                            @RequestParam(required = false) String productId,
                                            @RequestParam(required = false) Instant dateFrom,
                                            @RequestParam(required = false) Instant dateTo) {
        byte[] content = inventoryReportingService.exportCsv(reportType, warehouseId, productId, dateFrom, dateTo);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(reportType + ".csv")
                        .build().toString())
                .body(content);
    }
}
