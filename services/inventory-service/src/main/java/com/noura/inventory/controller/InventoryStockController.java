package com.noura.inventory.controller;

import com.noura.inventory.common.ApiResponse;
import com.noura.inventory.common.PageResponse;
import com.noura.inventory.dto.stock.AdjustmentMovementRequest;
import com.noura.inventory.dto.stock.StockAdjustmentRequest;
import com.noura.inventory.dto.stock.StockDeductionRequest;
import com.noura.inventory.dto.stock.StockLevelResponse;
import com.noura.inventory.dto.stock.StockOperationResponse;
import com.noura.inventory.dto.stock.StockReleaseReservationRequest;
import com.noura.inventory.dto.stock.StockReservationRequest;
import com.noura.inventory.service.InventoryStockService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * REST controller for stock visibility and stock mutations.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1")
public class InventoryStockController {

    private final InventoryStockService inventoryStockService;

    /**
     * Lists stock levels with optional filtering and pagination.
     *
     * @param productId optional product filter
     * @param warehouseId optional warehouse/location filter
     * @param binId optional bin filter
     * @param batchId optional batch filter
     * @param lowStockOnly optional low-stock filter
     * @param page zero-based page index
     * @param size page size
     * @param sortBy sort field alias
     * @param direction sort direction
     * @param request servlet request
     * @return paged stock-level response envelope
     */
    @GetMapping("/stock-levels")
    public ApiResponse<PageResponse<StockLevelResponse>> listStockLevels(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false, name = "warehouseId") UUID warehouseId,
            @RequestParam(required = false) UUID binId,
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) Boolean lowStockOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest request
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseDirection(direction), mapSortField(sortBy)));
        Page<StockLevelResponse> result = inventoryStockService.listStockLevels(
                productId,
                warehouseId,
                binId,
                batchId,
                lowStockOnly,
                pageable
        );
        return ApiResponse.ok("Stock levels", PageResponse.from(result), request.getRequestURI());
    }

    /**
     * Returns all stock levels for a product.
     *
     * @param productId product identifier
     * @param request servlet request
     * @return stock-level list envelope
     */
    @GetMapping("/stock-levels/products/{productId}")
    public ApiResponse<List<StockLevelResponse>> stockByProduct(
            @PathVariable UUID productId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Stock levels by product",
                inventoryStockService.getByProduct(productId),
                request.getRequestURI()
        );
    }

    /**
     * Returns stock level by product and location.
     *
     * @param productId product identifier
     * @param locationId warehouse/location identifier
     * @param request servlet request
     * @return stock-level envelope
     */
    @GetMapping("/stock-levels/products/{productId}/locations/{locationId}")
    public ApiResponse<StockLevelResponse> stockByProductAndLocation(
            @PathVariable UUID productId,
            @PathVariable UUID locationId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Stock level by product and location",
                inventoryStockService.getByProductAndLocation(productId, locationId),
                request.getRequestURI()
        );
    }

    /**
     * Returns paged low-stock entries.
     *
     * @param page zero-based page index
     * @param size page size
     * @param request servlet request
     * @return low-stock page envelope
     */
    @GetMapping("/stock-levels/low-stock")
    public ApiResponse<PageResponse<StockLevelResponse>> lowStock(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            HttpServletRequest request
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "quantityAvailable"));
        Page<StockLevelResponse> result = inventoryStockService.getLowStock(pageable);
        return ApiResponse.ok("Low stock levels", PageResponse.from(result), request.getRequestURI());
    }

    /**
     * Applies a single stock adjustment.
     *
     * @param requestBody adjustment command
     * @param actorUserId optional actor identity forwarded by gateway
     * @param request servlet request
     * @return mutation result envelope
     */
    @PostMapping("/stock-levels/adjustments")
    public ApiResponse<StockOperationResponse> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest requestBody,
            @RequestHeader(value = "X-Auth-Subject", required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Stock adjusted",
                inventoryStockService.adjust(requestBody, actorUserId),
                request.getRequestURI()
        );
    }

    /**
     * Compatibility endpoint for legacy bulk adjustment payload.
     *
     * @param requestBody legacy movement request
     * @param actorUserId optional actor identity forwarded by gateway
     * @param request servlet request
     * @return line-by-line mutation results
     */
    @PostMapping("/movements/adjustments")
    public ApiResponse<List<StockOperationResponse>> adjustStockCompatibility(
            @Valid @RequestBody AdjustmentMovementRequest requestBody,
            @RequestHeader(value = "X-Auth-Subject", required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Stock adjusted",
                inventoryStockService.adjustCompatibility(requestBody, actorUserId),
                request.getRequestURI()
        );
    }

    /**
     * Reserves available stock.
     *
     * @param requestBody reservation command
     * @param actorUserId optional actor identity forwarded by gateway
     * @param request servlet request
     * @return mutation result envelope
     */
    @PostMapping("/stock-levels/reservations")
    public ApiResponse<StockOperationResponse> reserveStock(
            @Valid @RequestBody StockReservationRequest requestBody,
            @RequestHeader(value = "X-Auth-Subject", required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Stock reserved",
                inventoryStockService.reserve(requestBody, actorUserId),
                request.getRequestURI()
        );
    }

    /**
     * Releases previously reserved stock.
     *
     * @param requestBody release command
     * @param actorUserId optional actor identity forwarded by gateway
     * @param request servlet request
     * @return mutation result envelope
     */
    @PostMapping("/stock-levels/reservations/release")
    public ApiResponse<StockOperationResponse> releaseReservation(
            @Valid @RequestBody StockReleaseReservationRequest requestBody,
            @RequestHeader(value = "X-Auth-Subject", required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Reservation released",
                inventoryStockService.releaseReservation(requestBody, actorUserId),
                request.getRequestURI()
        );
    }

    /**
     * Deducts stock from either reserved or available pool.
     *
     * @param requestBody deduction command
     * @param actorUserId optional actor identity forwarded by gateway
     * @param request servlet request
     * @return mutation result envelope
     */
    @PostMapping("/stock-levels/deductions")
    public ApiResponse<StockOperationResponse> deductStock(
            @Valid @RequestBody StockDeductionRequest requestBody,
            @RequestHeader(value = "X-Auth-Subject", required = false) String actorUserId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Stock deducted",
                inventoryStockService.deduct(requestBody, actorUserId),
                request.getRequestURI()
        );
    }

    /**
     * Parses direction query parameter into Spring sort direction.
     *
     * @param rawDirection incoming direction parameter
     * @return sort direction (defaults to DESC)
     */
    private Sort.Direction parseDirection(String rawDirection) {
        return "asc".equalsIgnoreCase(rawDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    /**
     * Maps public sort aliases to entity field names.
     *
     * @param rawSortBy incoming sort alias
     * @return internal entity field name
     */
    private String mapSortField(String rawSortBy) {
        if (rawSortBy == null || rawSortBy.isBlank()) {
            return "updatedAt";
        }
        String normalized = rawSortBy.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "createdat" -> "createdAt";
            case "updatedat" -> "updatedAt";
            case "quantityonhand" -> "quantityOnHand";
            case "quantityreserved" -> "quantityReserved";
            case "quantityavailable" -> "quantityAvailable";
            case "lastmovementat" -> "lastMovementAt";
            case "status", "stockstatus" -> "stockStatus";
            default -> "updatedAt";
        };
    }
}
