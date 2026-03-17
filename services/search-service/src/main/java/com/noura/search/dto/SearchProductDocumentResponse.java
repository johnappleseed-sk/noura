package com.noura.search.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Search projection document response returned by internal indexing APIs.
 *
 * @param productId product identifier
 * @param name indexed product name
 * @param active projected active flag
 * @param indexedAt search indexing timestamp
 * @param sourceUpdatedAt source-system update timestamp
 */
public record SearchProductDocumentResponse(
        UUID productId,
        String name,
        boolean active,
        Instant indexedAt,
        Instant sourceUpdatedAt
) {
}
