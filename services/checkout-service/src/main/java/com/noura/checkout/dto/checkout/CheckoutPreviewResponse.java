package com.noura.checkout.dto.checkout;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Checkout preview response model.
 *
 * @param cartId active cart identifier
 * @param customerRef customer reference from request context
 * @param storeId resolved store/location identifier
 * @param addressId resolved shipping address identifier
 * @param valid indicates whether checkout can proceed
 * @param lines line-level validation details
 * @param issues global checkout issues
 * @param totals checkout totals summary
 * @param shippingAddress resolved shipping address snapshot
 * @param preparedAt preview preparation timestamp
 */
public record CheckoutPreviewResponse(
        UUID cartId,
        String customerRef,
        UUID storeId,
        UUID addressId,
        boolean valid,
        List<CheckoutLineValidationResponse> lines,
        List<CheckoutIssueResponse> issues,
        CheckoutTotalsResponse totals,
        ShippingAddressSnapshotDto shippingAddress,
        Instant preparedAt
) {
}

