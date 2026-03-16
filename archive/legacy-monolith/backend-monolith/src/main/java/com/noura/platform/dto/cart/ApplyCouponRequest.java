package com.noura.platform.dto.cart;

import jakarta.validation.constraints.NotBlank;

public record ApplyCouponRequest(@NotBlank String couponCode) {
}
