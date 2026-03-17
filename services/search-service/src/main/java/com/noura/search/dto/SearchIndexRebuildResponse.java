package com.noura.search.dto;

import java.time.Instant;

/**
 * Internal response describing one search-index rebuild execution.
 *
 * @param provider provider code used for rebuild
 * @param indexedCount number of indexed product documents
 * @param rebuiltAt rebuild completion timestamp
 */
public record SearchIndexRebuildResponse(
        String provider,
        int indexedCount,
        Instant rebuiltAt
) {
}
