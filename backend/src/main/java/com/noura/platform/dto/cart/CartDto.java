package com.noura.platform.dto.cart;

import java.util.List;
import java.util.UUID;

public record CartDto(
        UUID cartId,
        UUID storeId,
        List<CartItemDto> items,
        CartTotalsDto totals
) {
}
