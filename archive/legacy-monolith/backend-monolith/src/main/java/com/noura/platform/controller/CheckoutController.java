package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.order.CheckoutConfirmRequest;
import com.noura.platform.dto.order.CheckoutPaymentRequest;
import com.noura.platform.dto.order.CheckoutRequest;
import com.noura.platform.dto.order.CheckoutShippingRequest;
import com.noura.platform.dto.order.CheckoutStepPreviewDto;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.service.UnifiedOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/checkout")
public class CheckoutController {

    private final UnifiedOrderService unifiedOrderService;

    /**
     * Executes review step.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/steps/review")
    public ApiResponse<CheckoutStepPreviewDto> reviewStep(HttpServletRequest http) {
        return ApiResponse.ok("Checkout review step", unifiedOrderService.reviewCheckoutStep(), http.getRequestURI());
    }

    /**
     * Executes shipping step.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/steps/shipping")
    public ApiResponse<CheckoutStepPreviewDto> shippingStep(
            @Valid @RequestBody CheckoutShippingRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Checkout shipping step", unifiedOrderService.shippingCheckoutStep(request), http.getRequestURI());
    }

    /**
     * Executes payment step.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/steps/payment")
    public ApiResponse<CheckoutStepPreviewDto> paymentStep(
            @Valid @RequestBody CheckoutPaymentRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Checkout payment step", unifiedOrderService.paymentCheckoutStep(request), http.getRequestURI());
    }

    /**
     * Executes confirm.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/steps/confirm")
    public ResponseEntity<ApiResponse<OrderDto>> confirm(
            @Valid @RequestBody CheckoutConfirmRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order confirmed", unifiedOrderService.confirmCheckout(request), http.getRequestURI()));
    }

    /**
     * Executes direct checkout.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> directCheckout(
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order confirmed", unifiedOrderService.checkout(request), http.getRequestURI()));
    }
}
