package com.noura.order.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Order line response DTO.
 *
 * @param id line identifier
 * @param lineNumber line number
 * @param productId product identifier
 * @param variantId variant identifier
 * @param sku sku code
 * @param productName product display name
 * @param variantName variant display name
 * @param quantity quantity
 * @param unitPrice captured unit price
 * @param lineTotal captured line total
 */
public record OrderItemResponse(
        UUID id,
        int lineNumber,
        UUID productId,
        UUID variantId,
        String sku,
        String productName,
        String variantName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}

