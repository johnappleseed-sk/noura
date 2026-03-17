package com.noura.search.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Internal batch upsert payload for search product documents.
 *
 * @param products product documents to upsert
 */
public record ProductSearchDocumentBatchUpsertRequest(
        @NotEmpty(message = "products is required")
        @Size(max = 500, message = "products must contain 500 items or fewer")
        List<@Valid ProductSearchDocumentUpsertRequest> products
) {
}
