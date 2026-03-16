package com.noura.pricing.dto.price;

import java.util.List;
import java.util.UUID;

/**
 * Response model for bulk product price resolution.
 *
 * @param prices resolved prices for products with active price records
 * @param missingProductIds product identifiers without active price records
 */
public record BulkPriceResolutionResponse(
        List<PriceResolutionResponse> prices,
        List<UUID> missingProductIds
) {
}

