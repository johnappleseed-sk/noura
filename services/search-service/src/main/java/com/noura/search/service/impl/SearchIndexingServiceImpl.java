package com.noura.search.service.impl;

import com.noura.search.domain.entity.SearchProductDocument;
import com.noura.search.dto.ProductSearchDocumentBatchUpsertRequest;
import com.noura.search.dto.ProductSearchDocumentUpsertRequest;
import com.noura.search.dto.SearchIndexRebuildResponse;
import com.noura.search.dto.SearchProductDocumentResponse;
import com.noura.search.provider.ProductSearchIndexProvider;
import com.noura.search.service.SearchIndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link SearchIndexingService}.
 */
@Service
@RequiredArgsConstructor
public class SearchIndexingServiceImpl implements SearchIndexingService {

    private final ProductSearchIndexProvider productSearchIndexProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<SearchProductDocumentResponse> upsertProductDocuments(ProductSearchDocumentBatchUpsertRequest request) {
        List<SearchProductDocument> documents = request.products().stream()
                .map(this::toDocument)
                .toList();
        return productSearchIndexProvider.upsertDocuments(documents).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public SearchIndexRebuildResponse rebuildProductIndex() {
        int indexedCount = productSearchIndexProvider.rebuildProductIndex();
        return new SearchIndexRebuildResponse(
                productSearchIndexProvider.providerCode(),
                indexedCount,
                Instant.now()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteProductDocument(UUID productId) {
        productSearchIndexProvider.deleteDocument(productId);
    }

    /**
     * Maps one internal upsert request into a search product document.
     *
     * @param request internal upsert request
     * @return search document
     */
    private SearchProductDocument toDocument(ProductSearchDocumentUpsertRequest request) {
        SearchProductDocument document = new SearchProductDocument();
        document.setProductId(request.productId());
        document.setProductCode(request.productCode());
        document.setName(request.name());
        document.setSlug(request.slug());
        document.setCategoryId(request.categoryId());
        document.setCategoryName(request.categoryName());
        document.setBrandId(request.brandId());
        document.setBrandName(request.brandName());
        document.setShortDescription(request.shortDescription());
        document.setActive(request.active());
        document.setTrending(request.trending());
        document.setPopularityScore(request.popularityScore());
        document.setAverageRating(request.averageRating());
        document.setReviewCount(request.reviewCount());
        document.setSourceUpdatedAt(request.sourceUpdatedAt());
        return document;
    }

    /**
     * Maps one indexed document into the internal response contract.
     *
     * @param document persisted search document
     * @return response DTO
     */
    private SearchProductDocumentResponse toResponse(SearchProductDocument document) {
        return new SearchProductDocumentResponse(
                document.getProductId(),
                document.getName(),
                document.isActive(),
                document.getIndexedAt(),
                document.getSourceUpdatedAt()
        );
    }
}
