package com.noura.order.dto.order;

import java.util.UUID;

/**
 * Summary returned after rebuilding the active cart from one previous order.
 *
 * @param orderId source order identifier
 * @param rebuiltItemCount number of cart lines re-added from the order
 * @param replacedExistingCart whether the current cart was cleared before re-adding items
 */
public record QuickReorderResponse(
        UUID orderId,
        int rebuiltItemCount,
        boolean replacedExistingCart
) {
}
