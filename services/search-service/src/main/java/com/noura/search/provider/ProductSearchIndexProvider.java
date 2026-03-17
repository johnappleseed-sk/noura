package com.noura.search.provider;

import com.noura.search.domain.entity.SearchProductDocument;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * Provider abstraction for search index storage and query execution.
 *
 * <p>This boundary is adapted from the archived monolith's search adapter concept so search-service
 * can keep stable service contracts while the backing implementation moves from PostgreSQL
 * projections to OpenSearch later.</p>
 */
public interface ProductSearchIndexProvider {

    /**
     * Returns the provider code for diagnostics and internal rebuild reporting.
     *
     * @return provider code
     */
    String providerCode();

    /**
     * Executes product search over indexed documents.
     *
     * @param query free-text query
     * @param categoryId optional category filter
     * @param brandId optional brand filter
     * @param page zero-based page index
     * @param size page size
     * @return paged document hits
     */
    Page<SearchProductDocument> searchProducts(String query, UUID categoryId, UUID brandId, int page, int size);

    /**
     * Returns predictive suggestions for the supplied query text and scope.
     *
     * @param query query text
     * @param scope suggestion scope
     * @return suggestion list
     */
    List<SearchSuggestionDto> predictive(String query, String scope);

    /**
     * Returns current trend tags from indexed documents.
     *
     * @return trend tag list
     */
    List<TrendTagDto> trendTags();

    /**
     * Upserts one or more product documents into the index.
     *
     * @param documents source documents
     * @return persisted documents
     */
    List<SearchProductDocument> upsertDocuments(List<SearchProductDocument> documents);

    /**
     * Rebuilds the search projection from canonical catalog source tables.
     *
     * @return number of indexed product documents
     */
    int rebuildProductIndex();

    /**
     * Deletes one indexed product document.
     *
     * @param productId product identifier
     */
    void deleteDocument(UUID productId);
}
