package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.inventory.dto.stock.StockLevelFilter;
import com.noura.platform.inventory.dto.stock.StockLevelResponse;
import com.noura.platform.inventory.service.StockLevelService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/stock-levels")
public class StockLevelController {

    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "updatedAt", "lastMovementAt", "quantityOnHand", "quantityAvailable", "quantityReserved");

    private final StockLevelService stockLevelService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<PageResponse<StockLevelResponse>> listStockLevels(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String binId,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) Boolean lowStockOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = InventoryPageRequestFactory.of(page, size, sortBy, direction, ALLOWED_SORTS, "updatedAt");
        Page<StockLevelResponse> result = stockLevelService.listStockLevels(
                new StockLevelFilter(productId, warehouseId, binId, batchId, lowStockOnly),
                pageable
        );
        return ApiResponse.ok("Stock levels", PageResponse.from(result), http.getRequestURI());
    }
}
