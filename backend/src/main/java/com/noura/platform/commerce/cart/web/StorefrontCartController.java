package com.noura.platform.commerce.cart.web;

import com.noura.platform.commerce.api.v1.dto.common.ApiEnvelope;
import com.noura.platform.commerce.api.v1.support.ApiTrace;
import com.noura.platform.commerce.cart.application.StorefrontCartService;
import com.noura.platform.commerce.customers.domain.StorefrontCustomerPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/storefront/v1/cart")
@Validated
public class StorefrontCartController {
    private final StorefrontCartService storefrontCartService;

    public StorefrontCartController(StorefrontCartService storefrontCartService) {
        this.storefrontCartService = storefrontCartService;
    }

    @GetMapping
    public ApiEnvelope<StorefrontCartService.CartDto> get(Authentication authentication, HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STOREFRONT_CART_GET_OK",
                "Cart fetched successfully.",
                storefrontCartService.getOrCreateCart(customerId),
                ApiTrace.resolve(request)
        );
    }

    @PostMapping("/items")
    public ApiEnvelope<StorefrontCartService.CartDto> addItem(@Valid @RequestBody AddCartItemRequest body,
                                                             Authentication authentication,
                                                             HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        var result = storefrontCartService.addItem(customerId, new StorefrontCartService.AddCartItemRequest(
                body.productId(),
                body.quantity()
        ));
        return ApiEnvelope.success(
                "STOREFRONT_CART_ITEM_ADD_OK",
                "Cart item added successfully.",
                result,
                ApiTrace.resolve(request)
        );
    }

    @PatchMapping("/items/{itemId}")
    public ApiEnvelope<StorefrontCartService.CartDto> updateItem(@PathVariable Long itemId,
                                                                @Valid @RequestBody UpdateCartItemRequest body,
                                                                Authentication authentication,
                                                                HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        var result = storefrontCartService.updateItem(customerId, itemId, new StorefrontCartService.UpdateCartItemRequest(body.quantity()));
        return ApiEnvelope.success(
                "STOREFRONT_CART_ITEM_UPDATE_OK",
                "Cart item updated successfully.",
                result,
                ApiTrace.resolve(request)
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ApiEnvelope<StorefrontCartService.CartDto> deleteItem(@PathVariable Long itemId,
                                                                Authentication authentication,
                                                                HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        storefrontCartService.removeItem(customerId, itemId);
        return ApiEnvelope.success(
                "STOREFRONT_CART_ITEM_DELETE_OK",
                "Cart item removed successfully.",
                storefrontCartService.getOrCreateCart(customerId),
                ApiTrace.resolve(request)
        );
    }

    @DeleteMapping
    public ApiEnvelope<StorefrontCartService.CartDto> clear(Authentication authentication, HttpServletRequest request) {
        Long customerId = resolveCustomerId(authentication);
        return ApiEnvelope.success(
                "STOREFRONT_CART_CLEAR_OK",
                "Cart cleared successfully.",
                storefrontCartService.clearCart(customerId),
                ApiTrace.resolve(request)
        );
    }

    private Long resolveCustomerId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof StorefrontCustomerPrincipal customerPrincipal)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Customer authentication required.");
        }
        if (customerPrincipal.id() == null || customerPrincipal.id() <= 0) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid customer identity.");
        }
        return customerPrincipal.id();
    }

    public record AddCartItemRequest(@NotNull @Min(1) Long productId, @Min(1) Integer quantity) {
    }

    public record UpdateCartItemRequest(@Min(1) Integer quantity) {
    }
}
