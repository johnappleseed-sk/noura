package com.noura.platform.dto.storefront;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StorefrontOrderSummaryDto(
        Long id,
        String orderNumber,
        String status,
        LocalDateTime placedAt,
        String currencyCode,
        BigDecimal grandTotal,
        int itemCount
) {
}
