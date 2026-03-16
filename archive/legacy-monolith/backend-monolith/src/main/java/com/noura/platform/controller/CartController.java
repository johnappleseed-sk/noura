package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.cart.AddCartItemRequest;
import com.noura.platform.dto.cart.CartResponse;
import com.noura.platform.dto.cart.UpdateCartItemRequest;
import com.noura.platform.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/cart", "${app.api.version-prefix:/api/v1}/cart"})
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart(HttpServletRequest http) {
        return ApiResponse.ok("Cart", cartService.getCart(), http.getRequestURI());
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Item added to cart", cartService.addCartItem(request), http.getRequestURI());
    }

    @RequestMapping(value = "/items/{itemId}", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ApiResponse<CartResponse> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Cart item updated", cartService.updateCartItem(itemId, request), http.getRequestURI());
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable UUID itemId, HttpServletRequest http) {
        return ApiResponse.ok("Cart item removed", cartService.removeCartItem(itemId), http.getRequestURI());
    }

    @DeleteMapping
    public ApiResponse<CartResponse> clearCart(HttpServletRequest http) {
        return ApiResponse.ok("Cart cleared", cartService.clearCurrentCart(), http.getRequestURI());
    }
}
