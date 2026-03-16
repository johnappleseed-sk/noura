package com.noura.platform.dto.returns;

import java.math.BigDecimal;

public record ReturnItemDto(
        Long id,
        Long orderItemId,
        String productName,
        String sku,
        int quantityRequested,
        Integer quantityReceived,
        Integer quantityApproved,
        BigDecimal unitRefundAmount,
        BigDecimal lineRefundAmount,
        String conditionNotes,
        boolean restock
) {
}
