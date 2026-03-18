package com.noura.checkout.dto.checkout;

import java.time.Instant;
import java.util.List;

/**
 * Checkout validation response model.
 *
 * @param valid indicates whether checkout is valid for place-order
 * @param issues global checkout issues
 * @param preview resolved preview snapshot used for validation
 * @param validatedAt validation timestamp
 */
public record CheckoutValidationResponse(
        boolean valid,
        List<CheckoutIssueResponse> issues,
        CheckoutPreviewResponse preview,
        Instant validatedAt
) {
}

