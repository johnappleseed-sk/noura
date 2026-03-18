package com.noura.order.controller;

import com.noura.order.common.ApiResponse;
import com.noura.order.common.PageResponse;
import com.noura.order.controller.support.OrderRequestContextResolver;
import com.noura.order.domain.enums.OrderStatus;
import com.noura.order.domain.enums.RefundStatus;
import com.noura.order.dto.order.CreateOrderRequest;
import com.noura.order.dto.order.OrderResponse;
import com.noura.order.dto.order.OrderStatusEventResponse;
import com.noura.order.dto.order.QuickReorderResponse;
import com.noura.order.dto.order.UpdateOrderStatusRequest;
import com.noura.order.service.OrderService;
import com.noura.order.service.model.OrderRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for order creation, history, and admin order management.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping
public class OrderController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "orderNumber",
            "customerRef",
            "status",
            "refundStatus",
            "totalAmount",
            "createdAt",
            "updatedAt",
            "placedAt"
    );

    private final OrderService orderService;
    private final OrderRequestContextResolver contextResolver;

    /**
     * Creates an order from checkout payload.
     *
     * @param requestBody create order request
     * @param request current HTTP request
     * @return created order response envelope
     */
    @PostMapping({"/api/v1/orders", "/api/orders"})
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest requestBody,
            HttpServletRequest request
    ) {
        OrderRequestContext context = contextResolver.resolve(request);
        OrderResponse data = orderService.createOrder(context, requestBody);
        return ApiResponse.ok("Order created", data, request.getRequestURI());
    }

    /**
     * Retrieves one order by ID.
     *
     * @param orderId order identifier
     * @param request current HTTP request
     * @return order response envelope
     */
    @GetMapping({"/api/v1/orders/{orderId}", "/api/orders/{orderId}"})
    public ApiResponse<OrderResponse> getById(
            @PathVariable UUID orderId,
            HttpServletRequest request
    ) {
        OrderRequestContext context = contextResolver.resolve(request);
        OrderResponse data = orderService.getOrderById(context, orderId);
        return ApiResponse.ok("Order", data, request.getRequestURI());
    }

    /**
     * Retrieves timeline events for one order.
     *
     * @param orderId order identifier
     * @param request current HTTP request
     * @return timeline response envelope
     */
    @GetMapping({"/api/v1/orders/{orderId}/timeline", "/api/orders/{orderId}/timeline"})
    public ApiResponse<List<OrderStatusEventResponse>> getTimeline(
            @PathVariable UUID orderId,
            HttpServletRequest request
    ) {
        OrderRequestContext context = contextResolver.resolve(request);
        List<OrderStatusEventResponse> data = orderService.getOrderTimeline(context, orderId);
        return ApiResponse.ok("Order timeline", data, request.getRequestURI());
    }

    /**
     * Lists current customer order history.
     *
     * @param request current HTTP request
     * @return customer order list response envelope
     */
    @GetMapping({"/api/v1/account/orders", "/api/account/orders"})
    public ApiResponse<List<OrderResponse>> listCustomerOrders(HttpServletRequest request) {
        OrderRequestContext context = contextResolver.resolve(request);
        List<OrderResponse> data = orderService.listCustomerOrders(context);
        return ApiResponse.ok("Order history", data, request.getRequestURI());
    }

    /**
     * Rebuilds the current customer's cart from one previous order.
     *
     * @param orderId order identifier
     * @param authorizationHeader optional authorization header
     * @param request current HTTP request
     * @return quick-reorder response envelope
     */
    @PostMapping({"/api/v1/account/orders/{orderId}/quick-reorder", "/api/account/orders/{orderId}/quick-reorder"})
    public ApiResponse<QuickReorderResponse> quickReorder(
            @PathVariable UUID orderId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            HttpServletRequest request
    ) {
        OrderRequestContext context = contextResolver.resolve(request);
        QuickReorderResponse data = orderService.quickReorder(
                context,
                orderId,
                authorizationHeader,
                request.getHeader("X-Correlation-ID")
        );
        return ApiResponse.ok("Quick reorder prepared", data, request.getRequestURI());
    }

    /**
     * Lists admin-visible orders with pagination and filtering.
     *
     * @param page zero-based page number
     * @param size page size
     * @param sortBy sort field
     * @param direction sort direction
     * @param query optional query
     * @param status optional order status filter
     * @param refundStatus optional refund status filter
     * @param request current HTTP request
     * @return paginated order list response envelope
     */
    @GetMapping({"/api/v1/orders", "/api/orders"})
    public ApiResponse<PageResponse<OrderResponse>> listAdminOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "placedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) RefundStatus refundStatus,
            HttpServletRequest request
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseDirection(direction), mapSortField(sortBy)));
        OrderRequestContext context = contextResolver.resolve(request);
        Page<OrderResponse> data = orderService.listAdminOrders(context, pageable, query, status, refundStatus);
        return ApiResponse.ok("Orders", PageResponse.from(data), request.getRequestURI());
    }

    /**
     * Updates one order status.
     *
     * @param orderId order identifier
     * @param requestBody status update payload
     * @param request current HTTP request
     * @return updated order response envelope
     */
    @PatchMapping({"/api/v1/orders/{orderId}/status", "/api/orders/{orderId}/status"})
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest requestBody,
            HttpServletRequest request
    ) {
        OrderRequestContext context = contextResolver.resolve(request);
        OrderResponse data = orderService.updateOrderStatus(context, orderId, requestBody);
        return ApiResponse.ok("Order status updated", data, request.getRequestURI());
    }

    /**
     * Parses direction query parameter into Spring sort direction.
     *
     * @param rawDirection direction parameter
     * @return parsed direction, defaulting to DESC
     */
    private Sort.Direction parseDirection(String rawDirection) {
        return "asc".equalsIgnoreCase(rawDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    /**
     * Maps user-provided sort field to safe column names.
     *
     * @param rawSortField requested sort field
     * @return mapped sort field
     */
    private String mapSortField(String rawSortField) {
        if (rawSortField == null || rawSortField.isBlank()) {
            return "placedAt";
        }
        String candidate = rawSortField.trim();
        if (ALLOWED_SORT_FIELDS.contains(candidate)) {
            return candidate;
        }
        String normalized = candidate.toLowerCase(Locale.ROOT);
        if ("createdat".equals(normalized)) {
            return "createdAt";
        }
        if ("updatedat".equals(normalized)) {
            return "updatedAt";
        }
        if ("placedat".equals(normalized)) {
            return "placedAt";
        }
        if ("total".equals(normalized)) {
            return "totalAmount";
        }
        return "placedAt";
    }
}
