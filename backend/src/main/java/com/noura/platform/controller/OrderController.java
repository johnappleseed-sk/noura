package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.OrderTimelineEventDto;
import com.noura.platform.dto.order.UpdateOrderStatusRequest;
import com.noura.platform.service.UnifiedOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/orders")
public class OrderController {

    private final UnifiedOrderService unifiedOrderService;

    /**
     * Executes admin orders.
     *
     * @param page The pagination configuration.
     * @param size The size value.
     * @param sortBy The sort by value.
     * @param direction The direction value.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping
    public ApiResponse<PageResponse<OrderDto>> adminOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<OrderDto> orders = unifiedOrderService.adminOrders(pageable);
        return ApiResponse.ok("Orders", PageResponse.from(orders), http.getRequestURI());
    }

    /**
     * Retrieves by id.
     *
     * @param orderId The order id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDto> getById(@PathVariable UUID orderId, HttpServletRequest http) {
        return ApiResponse.ok("Order", unifiedOrderService.getPlatformOrderById(orderId), http.getRequestURI());
    }

    /**
     * Retrieves order timeline.
     *
     * @param orderId The order id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/{orderId}/timeline")
    public ApiResponse<List<OrderTimelineEventDto>> orderTimeline(@PathVariable UUID orderId, HttpServletRequest http) {
        return ApiResponse.ok("Order timeline", unifiedOrderService.orderTimeline(orderId), http.getRequestURI());
    }

    /**
     * Updates status.
     *
     * @param orderId The order id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderDto> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Order status updated", unifiedOrderService.updateOrderStatus(orderId, request), http.getRequestURI());
    }
}
