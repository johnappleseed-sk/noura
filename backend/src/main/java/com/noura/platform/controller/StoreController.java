package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.dto.store.StoreDto;
import com.noura.platform.dto.store.StoreRequest;
import com.noura.platform.service.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/stores")
public class StoreController {

    private final StoreService storeService;

    /**
     * Lists matching data.
     *
     * @param service The service value.
     * @param openNow The open now value.
     * @param page The pagination configuration.
     * @param size The size value.
     * @param sortBy The sort by value.
     * @param direction The direction value.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping
    public ApiResponse<PageResponse<StoreDto>> list(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) Boolean openNow,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<StoreDto> stores = storeService.listStores(service, openNow, pageable);
        return ApiResponse.ok("Stores", PageResponse.from(stores), http.getRequestURI());
    }

    /**
     * Executes nearest.
     *
     * @param latitude The latitude value.
     * @param longitude The longitude value.
     * @param limit The limit value.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/nearest")
    public ApiResponse<List<StoreDto>> nearest(
            @RequestParam("lat") @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @RequestParam("lng") @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @RequestParam(defaultValue = "5") @Min(1) @Max(25) int limit,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Nearest stores", storeService.findNearest(latitude, longitude, limit), http.getRequestURI());
    }

    /**
     * Sets preferred store.
     *
     * @param storeId The store id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("/preferred/{storeId}")
    public ApiResponse<Void> setPreferredStore(@PathVariable UUID storeId, HttpServletRequest http) {
        storeService.setPreferredStore(storeId);
        return ApiResponse.ok("Preferred store updated", null, http.getRequestURI());
    }

    /**
     * Creates resource.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StoreDto>> create(@Valid @RequestBody StoreRequest request, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Store created", storeService.createStore(request), http.getRequestURI()));
    }

    /**
     * Updates resource.
     *
     * @param storeId The store id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("/{storeId}")
    public ApiResponse<StoreDto> update(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Store updated", storeService.updateStore(storeId, request), http.getRequestURI());
    }

    /**
     * Deletes resource.
     *
     * @param storeId The store id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @DeleteMapping("/{storeId}")
    public ApiResponse<Void> delete(@PathVariable UUID storeId, HttpServletRequest http) {
        storeService.deleteStore(storeId);
        return ApiResponse.ok("Store deleted", null, http.getRequestURI());
    }
}
