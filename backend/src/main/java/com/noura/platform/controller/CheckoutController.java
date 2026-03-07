package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.cart.CartDto;
import com.noura.platform.dto.order.CheckoutRequest;
import com.noura.platform.dto.order.CheckoutStepPreviewDto;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.service.CartService;
import com.noura.platform.service.CheckoutService;
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

    private final CheckoutService checkoutService;
    private final CartService cartService;

    /**
     * Executes review step.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/steps/review")
    public ApiResponse<CheckoutStepPreviewDto> reviewStep(HttpServletRequest http) {
        CartDto cart = cartService.getMyCart();
        return ApiResponse.ok(
                "Checkout review step",
                new CheckoutStepPreviewDto("review", "shipping", "Validate items, coupon and totals before shipping.", cart),
                http.getRequestURI()
        );
    }

    /**
     * Executes shipping step.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/steps/shipping")
    public ApiResponse<CheckoutStepPreviewDto> shippingStep(HttpServletRequest http) {
        CartDto cart = cartService.getMyCart();
        return ApiResponse.ok(
                "Checkout shipping step",
                new CheckoutStepPreviewDto("shipping", "payment", "Select pickup vs delivery and verify address.", cart),
                http.getRequestURI()
        );
    }

    /**
     * Executes payment step.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/steps/payment")
    public ApiResponse<CheckoutStepPreviewDto> paymentStep(HttpServletRequest http) {
        CartDto cart = cartService.getMyCart();
        return ApiResponse.ok(
                "Checkout payment step",
                new CheckoutStepPreviewDto("payment", "confirm", "Attach payment reference or B2B invoice option.", cart),
                http.getRequestURI()
        );
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
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order confirmed", checkoutService.checkout(request), http.getRequestURI()));
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
                .body(ApiResponse.ok("Order confirmed", checkoutService.checkout(request), http.getRequestURI()));
    }
}
