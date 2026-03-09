package com.noura.platform.dto.returns;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReturnRequestDto(
        Long id,
        String returnNumber,
        Long orderId,
        String orderNumber,
        Long customerId,
        String status,
        String reason,
        String reasonDetails,
        String customerNotes,
        String staffNotes,
        BigDecimal refundAmount,
        String currencyCode,
        String returnTrackingNumber,
        String returnCarrier,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime itemsReceivedAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ReturnItemDto> items
) {
}
