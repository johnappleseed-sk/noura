package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.cart.*;
import com.noura.platform.service.UnifiedOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/cart")
public class CartController {

    private final UnifiedOrderService unifiedOrderService;

    /**
     * Retrieves my cart.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping
    public ApiResponse<CartDto> myCart(HttpServletRequest http) {
        return ApiResponse.ok("Cart", unifiedOrderService.getMyCart(), http.getRequestURI());
    }

    /**
     * Adds item.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/items")
    public ApiResponse<CartDto> addItem(@Valid @RequestBody AddCartItemRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Item added to cart", unifiedOrderService.addCartItem(request), http.getRequestURI());
    }

    /**
     * Updates item.
     *
     * @param cartItemId The cart item id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("/items/{cartItemId}")
    public ApiResponse<CartDto> updateItem(
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Cart item updated", unifiedOrderService.updateCartItem(cartItemId, request), http.getRequestURI());
    }

    /**
     * Removes item.
     *
     * @param cartItemId The cart item id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<CartDto> removeItem(@PathVariable UUID cartItemId, HttpServletRequest http) {
        return ApiResponse.ok("Cart item removed", unifiedOrderService.removeCartItem(cartItemId), http.getRequestURI());
    }

    /**
     * Clears cart.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @DeleteMapping("/items")
    public ApiResponse<CartDto> clearItems(HttpServletRequest http) {
        return ApiResponse.ok("Cart cleared", unifiedOrderService.clearCart(), http.getRequestURI());
    }

    /**
     * Applies coupon.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/coupon")
    public ApiResponse<CartDto> applyCoupon(@Valid @RequestBody ApplyCouponRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Coupon applied", unifiedOrderService.applyCoupon(request), http.getRequestURI());
    }
}
