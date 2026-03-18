package com.noura.order.controller;

import com.noura.order.common.ApiResponse;
import com.noura.order.config.InternalApiProperties;
import com.noura.order.dto.order.OrderResponse;
import com.noura.order.dto.order.UpdateOrderStatusRequest;
import com.noura.order.exception.OrderOperationException;
import com.noura.order.service.OrderService;
import com.noura.order.service.model.OrderRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

/**
 * Trusted internal order lifecycle controller used by synchronous checkout orchestration.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/orders")
public class InternalOrderLifecycleController {

    private final OrderService orderService;
    private final InternalApiProperties internalApiProperties;

    /**
     * Applies one privileged order status transition without relying on forwarded admin roles.
     *
     * @param orderId order identifier
     * @param requestBody transition payload
     * @param providedApiKey optional internal API key
     * @param request current HTTP request
     * @return updated order response envelope
     */
    @PostMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest requestBody,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedApiKey,
            HttpServletRequest request
    ) {
        validateInternalApiKey(providedApiKey);
        OrderResponse data = orderService.updateOrderStatus(
                new OrderRequestContext(null, Set.of(), true),
                orderId,
                requestBody
        );
        return ApiResponse.ok("Order status updated", data, request.getRequestURI());
    }

    /**
     * Validates internal API key when one is configured.
     *
     * @param providedApiKey API key provided by caller
     */
    private void validateInternalApiKey(String providedApiKey) {
        String configuredApiKey = trimToNull(internalApiProperties.getApiKey());
        if (configuredApiKey == null) {
            return;
        }
        if (!configuredApiKey.equals(trimToNull(providedApiKey))) {
            throw new OrderOperationException(
                    HttpStatus.FORBIDDEN,
                    "INTERNAL_API_KEY_INVALID",
                    "Invalid internal API key"
            );
        }
    }

    /**
     * Trims input and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
