package com.noura.catalog.dto.product;

import java.util.UUID;

/**
 * Internal variant lookup payload used by compatibility adapters that still speak in variant IDs.
 *
 * @param variantId variant identifier
 * @param productId owning product identifier
 * @param sku variant SKU
 * @param variantName display name when available
 * @param active whether the variant is active
 */
public record VariantLookupResponse(
        UUID variantId,
        UUID productId,
        String sku,
        String variantName,
        boolean active
) {
}
