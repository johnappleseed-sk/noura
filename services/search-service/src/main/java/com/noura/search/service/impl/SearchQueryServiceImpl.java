package com.noura.search.service.impl;

import com.noura.search.domain.entity.SearchProductDocument;
import com.noura.search.dto.ProductSearchHitDto;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import com.noura.search.provider.ProductSearchIndexProvider;
import com.noura.search.service.SearchQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link SearchQueryService}.
 *
 * <p>The service stays thin by delegating query execution to a provider abstraction. That keeps the
 * external API stable if the backing implementation later moves from PostgreSQL projection queries
 * to OpenSearch or another dedicated indexing backend.</p>
 */
@Service
@RequiredArgsConstructor
public class SearchQueryServiceImpl implements SearchQueryService {

    private final ProductSearchIndexProvider productSearchIndexProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductSearchHitDto> searchProducts(String query, UUID categoryId, UUID brandId, int page, int size) {
        return productSearchIndexProvider.searchProducts(query, categoryId, brandId, page, size)
                .map(this::toProductSearchHit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<SearchSuggestionDto> predictive(String q, String scope) {
        return productSearchIndexProvider.predictive(q, scope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<TrendTagDto> trendTags() {
        return productSearchIndexProvider.trendTags();
    }

    /**
     * Maps one indexed document to the public product-search hit contract.
     *
     * @param document indexed product document
     * @return public search hit
     */
    private ProductSearchHitDto toProductSearchHit(SearchProductDocument document) {
        return new ProductSearchHitDto(
                document.getProductId(),
                document.getProductCode(),
                document.getName(),
                document.getSlug(),
                document.getCategoryId(),
                document.getCategoryName(),
                document.getBrandId(),
                document.getBrandName(),
                document.getAverageRating(),
                document.getReviewCount(),
                document.isTrending(),
                document.getPopularityScore()
        );
    }
}
