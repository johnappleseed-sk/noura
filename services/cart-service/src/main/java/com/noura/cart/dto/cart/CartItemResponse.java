package com.noura.cart.dto.cart;

import com.noura.cart.domain.enums.CartItemValidationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cart line item read model.
 *
 * @param id cart item identifier
 * @param productId product identifier
 * @param variantId optional variant identifier
 * @param storeId optional store/location identifier
 * @param productCode product code snapshot
 * @param productName product name snapshot
 * @param sku SKU snapshot
 * @param quantity quantity in cart
 * @param unitPrice effective unit price snapshot
 * @param lineTotal computed line total
 * @param availableQuantity latest known available quantity
 * @param validationStatus line validation snapshot status
 * @param validationMessage line validation detail
 * @param updatedAt line update timestamp
 */
public record CartItemResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        UUID storeId,
        String productCode,
        String productName,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        BigDecimal availableQuantity,
        CartItemValidationStatus validationStatus,
        String validationMessage,
        Instant updatedAt
) {
}
