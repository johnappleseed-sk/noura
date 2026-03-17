package com.noura.search.service;

import com.noura.search.dto.ProductSearchDocumentBatchUpsertRequest;
import com.noura.search.dto.SearchIndexRebuildResponse;
import com.noura.search.dto.SearchProductDocumentResponse;

import java.util.List;
import java.util.UUID;

/**
 * Internal indexing service for search projection writes and rebuilds.
 */
public interface SearchIndexingService {

    /**
     * Upserts a batch of product search documents.
     *
     * @param request batch request
     * @return persisted document responses
     */
    List<SearchProductDocumentResponse> upsertProductDocuments(ProductSearchDocumentBatchUpsertRequest request);

    /**
     * Rebuilds the search projection from canonical source tables.
     *
     * @return rebuild result
     */
    SearchIndexRebuildResponse rebuildProductIndex();

    /**
     * Deletes one product document from the search projection.
     *
     * @param productId product identifier
     */
    void deleteProductDocument(UUID productId);
}
