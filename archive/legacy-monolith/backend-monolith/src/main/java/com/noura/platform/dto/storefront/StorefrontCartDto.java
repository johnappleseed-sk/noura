package com.noura.platform.dto.storefront;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StorefrontCartDto(
        Long id,
        String status,
        String currencyCode,
        List<StorefrontCartItemDto> items,
        BigDecimal subtotal,
        int itemCount,
        LocalDateTime updatedAt
) {
}
