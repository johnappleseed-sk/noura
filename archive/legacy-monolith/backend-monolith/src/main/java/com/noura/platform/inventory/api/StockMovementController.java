package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.inventory.dto.stock.AdjustmentMovementRequest;
import com.noura.platform.inventory.dto.stock.InboundMovementRequest;
import com.noura.platform.inventory.dto.stock.OutboundMovementRequest;
import com.noura.platform.inventory.dto.stock.ReturnMovementRequest;
import com.noura.platform.inventory.dto.stock.StockMovementFilter;
import com.noura.platform.inventory.dto.stock.StockMovementResponse;
import com.noura.platform.inventory.dto.stock.TransferMovementRequest;
import com.noura.platform.inventory.service.StockMovementService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/movements")
public class StockMovementController {

    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "processedAt", "movementNumber", "movementType", "movementStatus");

    private final StockMovementService stockMovementService;

    @PostMapping("/inbound")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<StockMovementResponse>> receiveInbound(@Valid @RequestBody InboundMovementRequest request,
                                                                             HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Inbound movement completed", stockMovementService.receiveInbound(request), http.getRequestURI()));
    }

    @PostMapping("/outbound")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<StockMovementResponse>> shipOutbound(@Valid @RequestBody OutboundMovementRequest request,
                                                                           HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Outbound movement completed", stockMovementService.shipOutbound(request), http.getRequestURI()));
    }

    @PostMapping("/returns")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<StockMovementResponse>> returnStock(@Valid @RequestBody ReturnMovementRequest request,
                                                                          HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Return completed", stockMovementService.returnStock(request), http.getRequestURI()));
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<StockMovementResponse>> transferStock(@Valid @RequestBody TransferMovementRequest request,
                                                                            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transfer completed", stockMovementService.transferStock(request), http.getRequestURI()));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<StockMovementResponse>> adjustStock(@Valid @RequestBody AdjustmentMovementRequest request,
                                                                          HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Adjustment completed", stockMovementService.adjustStock(request), http.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<PageResponse<StockMovementResponse>> listMovements(
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
        Pageable pageable = InventoryPageRequestFactory.of(page, size, sortBy, direction, ALLOWED_SORTS, "processedAt");
        Page<StockMovementResponse> result = stockMovementService.listMovements(
                new StockMovementFilter(movementType, movementStatus, warehouseId, productId, referenceQuery, processedFrom, processedTo),
                pageable
        );
        return ApiResponse.ok("Stock movements", PageResponse.from(result), http.getRequestURI());
    }

    @GetMapping("/{movementId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<StockMovementResponse> getMovement(@PathVariable String movementId, HttpServletRequest http) {
        return ApiResponse.ok("Stock movement", stockMovementService.getMovement(movementId), http.getRequestURI());
    }
}
