package com.noura.platform.inventory.dto.batch;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BatchLotResponse(
        String id,
        String productId,
        String productSku,
        String productName,
        String lotNumber,
        String supplierBatchRef,
        LocalDate manufacturedAt,
        Instant receivedAt,
        LocalDate expiryDate,
        String status,
        String notes,
        BigDecimal quantityOnHand,
        BigDecimal quantityAvailable
) {
}
