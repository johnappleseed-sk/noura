package com.noura.order.dto.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Order line payload copied from checkout/cart into immutable order items.
 *
 * @param productId product identifier
 * @param variantId variant identifier
 * @param sku sku code
 * @param productName product display name
 * @param variantName variant display name
 * @param quantity requested quantity
 * @param unitPrice captured unit price
 * @param lineTotal captured line total, optional (computed if omitted)
 * @param itemSnapshot optional line snapshot map
 */
public record CreateOrderItemRequest(
        @NotNull UUID productId,
        UUID variantId,
        @Size(max = 120) String sku,
        @NotBlank @Size(max = 255) String productName,
        @Size(max = 160) String variantName,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
        @DecimalMin("0.0") BigDecimal lineTotal,
        Map<String, Object> itemSnapshot
) {
}

