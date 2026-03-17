package com.noura.search.controller;

import com.noura.search.common.ApiResponse;
import com.noura.search.config.InternalApiProperties;
import com.noura.search.dto.ProductSearchDocumentBatchUpsertRequest;
import com.noura.search.dto.SearchIndexRebuildResponse;
import com.noura.search.dto.SearchProductDocumentResponse;
import com.noura.search.exception.SearchOperationException;
import com.noura.search.service.SearchIndexingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Internal-only controller for search projection indexing operations.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/search/index")
public class InternalSearchIndexController {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final SearchIndexingService searchIndexingService;
    private final InternalApiProperties internalApiProperties;

    /**
     * Upserts one or more product documents into the search projection.
     *
     * @param internalApiKey internal API key header
     * @param requestBody batch upsert payload
     * @param request current request
     * @return persisted document responses
     */
    @PostMapping("/products")
    public ApiResponse<List<SearchProductDocumentResponse>> upsertProductDocuments(
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @Valid @RequestBody ProductSearchDocumentBatchUpsertRequest requestBody,
            HttpServletRequest request
    ) {
        assertInternalApiAccess(internalApiKey);
        List<SearchProductDocumentResponse> data = searchIndexingService.upsertProductDocuments(requestBody);
        return ApiResponse.ok("Search product documents indexed", data, request.getRequestURI());
    }

    /**
     * Rebuilds the product search projection from canonical source tables.
     *
     * @param internalApiKey internal API key header
     * @param request current request
     * @return rebuild summary
     */
    @PostMapping("/products/rebuild")
    public ApiResponse<SearchIndexRebuildResponse> rebuildProductIndex(
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            HttpServletRequest request
    ) {
        assertInternalApiAccess(internalApiKey);
        SearchIndexRebuildResponse data = searchIndexingService.rebuildProductIndex();
        return ApiResponse.ok("Search product index rebuilt", data, request.getRequestURI());
    }

    /**
     * Deletes one product document from the search projection.
     *
     * @param productId product identifier
     * @param internalApiKey internal API key header
     * @param request current request
     * @return delete result
     */
    @DeleteMapping("/products/{productId}")
    public ApiResponse<Void> deleteProductDocument(
            @PathVariable UUID productId,
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            HttpServletRequest request
    ) {
        assertInternalApiAccess(internalApiKey);
        searchIndexingService.deleteProductDocument(productId);
        return ApiResponse.ok("Search product document deleted", null, request.getRequestURI());
    }

    /**
     * Validates internal indexing access using the configured shared API key.
     *
     * @param providedApiKey provided internal API key
     */
    private void assertInternalApiAccess(String providedApiKey) {
        String configuredApiKey = normalizeNullable(internalApiProperties.getApiKey());
        if (configuredApiKey == null) {
            throw new SearchOperationException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SEARCH_INTERNAL_API_DISABLED",
                    "Search internal indexing API is disabled because no internal API key is configured"
            );
        }
        if (!configuredApiKey.equals(normalizeNullable(providedApiKey))) {
            throw new SearchOperationException(
                    HttpStatus.FORBIDDEN,
                    "SEARCH_INDEX_FORBIDDEN",
                    "Search internal indexing API requires a valid internal API key"
            );
        }
    }

    /**
     * Trims text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
