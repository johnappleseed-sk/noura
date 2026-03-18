package com.noura.cart.controller;

import com.noura.cart.common.ApiResponse;
import com.noura.cart.dto.cart.AddCartItemRequest;
import com.noura.cart.dto.cart.ApplyCouponRequest;
import com.noura.cart.dto.cart.CartResponse;
import com.noura.cart.dto.cart.MergeGuestCartRequest;
import com.noura.cart.dto.cart.RefreshCartRequest;
import com.noura.cart.dto.cart.UpdateCartItemQuantityRequest;
import com.noura.cart.service.CartService;
import com.noura.cart.service.model.CartContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Cart API controller with compatibility routes for existing storefront clients.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/cart", "/api/cart"})
public class CartController {

    private static final String HEADER_AUTH_SUBJECT = "X-Auth-Subject";
    private static final String HEADER_CART_TOKEN = "X-Cart-Token";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    private final CartService cartService;

    /**
     * Returns active cart for current customer or guest context.
     *
     * @param authSubject optional auth subject forwarded by gateway
     * @param cartToken optional guest cart token
     * @param authorization optional authorization header
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @return cart response envelope
     */
    @GetMapping
    public ApiResponse<CartResponse> getCart(
            @RequestHeader(value = HEADER_AUTH_SUBJECT, required = false) String authSubject,
            @RequestHeader(value = HEADER_CART_TOKEN, required = false) String cartToken,
            @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String authorization,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        CartContext context = resolveContext(authSubject, cartToken, authorization);
        CartResponse data = cartService.getCart(context);
        attachGuestTokenHeader(data, httpResponse);
        return ApiResponse.ok("Cart", data, httpRequest.getRequestURI());
    }

    /**
     * Adds one line item to current cart.
     *
     * @param requestBody add-item command payload
     * @param authSubject optional auth subject forwarded by gateway
     * @param cartToken optional guest cart token
     * @param authorization optional authorization header
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @return updated cart response envelope
     */
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @Valid @RequestBody AddCartItemRequest requestBody,
            @RequestHeader(value = HEADER_AUTH_SUBJECT, required = false) String authSubject,
            @RequestHeader(value = HEADER_CART_TOKEN, required = false) String cartToken,
            @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String authorization,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        CartContext context = resolveContext(authSubject, cartToken, authorization);
        CartResponse data = cartService.addItem(context, requestBody);
        attachGuestTokenHeader(data, httpResponse);
        return ApiResponse.ok("Item added to cart", data, httpRequest.getRequestURI());
    }

    /**
     * Replaces quantity for one line item.
     *
     * @param itemId target line item identifier
     * @param requestBody quantity update payload
     * @param authSubject optional auth subject forwarded by gateway
     * @param cartToken optional guest cart token
     * @param authorization optional authorization header
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @return updated cart response envelope
     */
    @PutMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateQuantity(
            @PathVariable @NotNull UUID itemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest requestBody,
            @RequestHeader(value = HEADER_AUTH_SUBJECT, required = false) String authSubject,
            @RequestHeader(value = HEADER_CART_TOKEN, required = false) String cartToken,
            @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String authorization,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        CartContext context = resolveContext(authSubject, cartToken, authorization);
        CartResponse data = cartService.updateItemQuantity(context, itemId, requestBody);
        attachGuestTokenHeader(data, httpResponse);
        return ApiResponse.ok("Cart item updated", data, httpRequest.getRequestURI());
    }

    /**
     * Removes one cart line item.
     *
     * @param itemId target line item identifier
     * @param authSubject optional auth subject forwarded by gateway
     * @param cartToken optional guest cart token
     * @param authorization optional authorization header
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @return updated cart response envelope
     */
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(
            @PathVariable @NotNull UUID itemId,
            @RequestHeader(value = HEADER_AUTH_SUBJECT, required = false) String authSubject,
            @RequestHeader(value = HEADER_CART_TOKEN, required = false) String cartToken,
            @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String authorization,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        CartContext context = resolveContext(authSubject, cartToken, authorization);
        CartResponse data = cartService.removeItem(context, itemId);
        attachGuestTokenHeader(data, httpResponse);
        return ApiResponse.ok("Cart item removed", data, httpRequest.getRequestURI());
    }

    /**
     * Clears all cart line items.
     *
     * @param authSubject optional auth subject forwarded by gateway
     * @param cartToken optional guest cart token
     * @param authorization optional authorization header
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @return cleared cart response envelope
     */
    @DeleteMapping({"", "/items"})
    public ApiResponse<CartResponse> clearCart(
            @RequestHeader(value = HEADER_AUTH_SUBJECT, required = false) String authSubject,
            @RequestHeader(value = HEADER_CART_TOKEN, required = false) String cartToken,
            @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String authorization,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        CartContext context = resolveContext(authSubject, cartToken, authorization);
        CartResponse data = cartService.clear(context);
        attachGuestTokenHeader(data, httpResponse);
        return ApiResponse.ok("Cart cleared", data, httpRequest.getRequestURI());
    }

    /**
     * Merges guest cart into current customer cart.
     *
     * @param requestBody merge request body
     * @param authSubject optional auth subject forwarded by gateway
     * @param cartToken optional guest cart token
     * @param authorization optional authorization header
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @return merged cart response envelope
     */
    @PostMapping("/merge")
    public ApiResponse<CartResponse> mergeGuestCart(
            @Valid @RequestBody MergeGuestCartRequest requestBody,
            @RequestHeader(value = HEADER_AUTH_SUBJECT, required = false) String authSubject,
            @RequestHeader(value = HEADER_CART_TOKEN, required = false) String cartToken,
            @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String authorization,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        CartContext context = resolveContext(authSubject, cartToken, authorization);
        CartResponse data = cartService.mergeGuestCart(context, requestBody);
        attachGuestTokenHeader(data, httpResponse);
        return ApiResponse.ok("Guest cart merged", data, httpRequest.getRequestURI());
    }

    /**
     * Revalidates cart lines against current pricing and availability.
     *
     * @param requestBody refresh request payload (optional)
     * @param authSubject optional auth subject forwarded by gateway
     * @param cartToken optional guest cart token
     * @param authorization optional authorization header
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @return refreshed cart response envelope
     */
    @PostMapping("/refresh")
    public ApiResponse<CartResponse> refresh(
            @RequestBody(required = false) RefreshCartRequest requestBody,
            @RequestHeader(value = HEADER_AUTH_SUBJECT, required = false) String authSubject,
            @RequestHeader(value = HEADER_CART_TOKEN, required = false) String cartToken,
            @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String authorization,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        CartContext context = resolveContext(authSubject, cartToken, authorization);
        boolean strict = requestBody != null && Boolean.TRUE.equals(requestBody.strict());
        CartResponse data = cartService.refresh(context, strict);
        attachGuestTokenHeader(data, httpResponse);
        return ApiResponse.ok("Cart refreshed", data, httpRequest.getRequestURI());
    }

    /**
     * Applies one coupon code to the current cart and refreshes cart totals.
     *
     * @param requestBody coupon apply payload
     * @param authSubject optional auth subject forwarded by gateway
     * @param cartToken optional guest cart token
     * @param authorization optional authorization header
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @return updated cart response envelope
     */
    @PostMapping("/coupon")
    public ApiResponse<CartResponse> applyCoupon(
            @Valid @RequestBody ApplyCouponRequest requestBody,
            @RequestHeader(value = HEADER_AUTH_SUBJECT, required = false) String authSubject,
            @RequestHeader(value = HEADER_CART_TOKEN, required = false) String cartToken,
            @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String authorization,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        CartContext context = resolveContext(authSubject, cartToken, authorization);
        CartResponse data = cartService.applyCoupon(context, requestBody);
        attachGuestTokenHeader(data, httpResponse);
        return ApiResponse.ok("Coupon applied", data, httpRequest.getRequestURI());
    }

    /**
     * Removes any applied coupon from the current cart.
     *
     * @param authSubject optional auth subject forwarded by gateway
     * @param cartToken optional guest cart token
     * @param authorization optional authorization header
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @return updated cart response envelope
     */
    @DeleteMapping("/coupon")
    public ApiResponse<CartResponse> removeCoupon(
            @RequestHeader(value = HEADER_AUTH_SUBJECT, required = false) String authSubject,
            @RequestHeader(value = HEADER_CART_TOKEN, required = false) String cartToken,
            @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String authorization,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        CartContext context = resolveContext(authSubject, cartToken, authorization);
        CartResponse data = cartService.removeCoupon(context);
        attachGuestTokenHeader(data, httpResponse);
        return ApiResponse.ok("Coupon removed", data, httpRequest.getRequestURI());
    }

    /**
     * Resolves ownership context from headers.
     *
     * @param authSubject optional gateway-forwarded user subject
     * @param cartToken optional guest token
     * @param authorization optional authorization header
     * @return resolved context
     */
    private CartContext resolveContext(String authSubject, String cartToken, String authorization) {
        String normalizedSubject = normalizeNullable(authSubject);
        if (normalizedSubject != null) {
            return CartContext.customer(normalizedSubject);
        }

        String authorizationFingerprint = resolveAuthorizationFingerprint(authorization);
        if (authorizationFingerprint != null) {
            return CartContext.customer(authorizationFingerprint);
        }

        String normalizedToken = normalizeNullable(cartToken);
        if (normalizedToken != null) {
            return CartContext.guest(normalizedToken);
        }

        return CartContext.guest("guest-" + UUID.randomUUID());
    }

    /**
     * Extracts stable customer fallback from bearer token by hashing raw token value.
     *
     * @param authorization optional authorization header
     * @return stable token fingerprint or null when header is absent/invalid
     */
    private String resolveAuthorizationFingerprint(String authorization) {
        String normalizedHeader = normalizeNullable(authorization);
        if (normalizedHeader == null || !normalizedHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String rawToken = normalizedHeader.substring(7).trim();
        if (rawToken.isEmpty()) {
            return null;
        }
        return "bearer-" + sha256(rawToken);
    }

    /**
     * Hashes a value using SHA-256.
     *
     * @param value source text
     * @return lowercase hex digest
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    /**
     * Normalizes nullable string values.
     *
     * @param value source value
     * @return trimmed value, or null when blank
     */
    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Writes guest token header for guest cart contexts.
     *
     * @param cart cart response
     * @param response HTTP response
     */
    private void attachGuestTokenHeader(CartResponse cart, HttpServletResponse response) {
        if (cart.guestToken() != null && !cart.guestToken().isBlank()) {
            response.setHeader(HEADER_CART_TOKEN, cart.guestToken());
        }
    }
}
