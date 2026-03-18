package com.noura.cart.dto.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Command payload for applying a promotion or coupon code to the current cart.
 *
 * @param couponCode promo/coupon code supplied by storefront
 */
public record ApplyCouponRequest(
        @NotBlank(message = "couponCode is required")
        @Size(max = 80, message = "couponCode must be at most 80 characters")
        String couponCode
) {
}
