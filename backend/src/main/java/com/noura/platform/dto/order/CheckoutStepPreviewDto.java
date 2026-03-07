package com.noura.platform.dto.order;

import com.noura.platform.dto.cart.CartDto;

public record CheckoutStepPreviewDto(
        String step,
        String nextStep,
        String message,
        CartDto cart
) {
}
