package com.noura.checkout.dto.checkout;

import java.time.Instant;
import java.util.List;

/**
 * Place-order orchestration response model.
 *
 * @param order created order summary
 * @param payment synchronized payment summary
 * @param reservedStock reserved stock movement summaries
 * @param idempotencyKey normalized idempotency key used for this command
 * @param replayed indicates this response was replayed from idempotency storage
 * @param placedAt checkout placement timestamp
 * @param summaryMessage summary message for UI surfaces
 */
public record CheckoutPlaceOrderResponse(
        CheckoutOrderSummaryResponse order,
        CheckoutPaymentSummaryResponse payment,
        List<ReservedStockResponse> reservedStock,
        String idempotencyKey,
        boolean replayed,
        Instant placedAt,
        String summaryMessage
) {
}
