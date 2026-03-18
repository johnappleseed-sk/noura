package com.noura.checkout.dto.checkout;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Reserved stock line returned for successful place-order orchestration.
 *
 * @param productId product identifier
 * @param locationId location identifier
 * @param quantity reserved quantity
 * @param movementId inventory movement identifier
 */
public record ReservedStockResponse(
        UUID productId,
        UUID locationId,
        BigDecimal quantity,
        UUID movementId
) {
}

