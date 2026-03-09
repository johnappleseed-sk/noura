package com.noura.platform.dto.storefront;

public record StorefrontAddCartItemRequest(
        long productId,
        int quantity
) {
}
