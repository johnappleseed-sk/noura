package com.noura.search.provider;

import com.noura.search.domain.entity.SearchBrand;
import com.noura.search.domain.entity.SearchCategory;
import com.noura.search.domain.entity.SearchProduct;
import com.noura.search.domain.entity.SearchProductDocument;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import com.noura.search.repository.SearchBrandRepository;
import com.noura.search.repository.SearchCategoryRepository;
import com.noura.search.repository.SearchProductDocumentRepository;
import com.noura.search.repository.SearchProductRepository;
import com.noura.search.repository.SearchStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PostgreSQL-backed search index provider.
 *
 * <p>This provider owns the first standalone search implementation for NOURA. It queries the
 * search-service projection table for runtime traffic and rebuilds that projection from canonical
 * catalog tables until event-driven indexing is introduced.</p>
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.search", name = "provider", havingValue = "postgres", matchIfMissing = true)
public class PostgresProductSearchIndexProvider implements ProductSearchIndexProvider {

    private static final int MAX_RESULTS = 20;
    private static final int PRODUCT_LIMIT = 10;
    private static final int ENTITY_LIMIT = 5;

    private final SearchProductDocumentRepository documentRepository;
    private final SearchProductRepository sourceProductRepository;
    private final SearchBrandRepository sourceBrandRepository;
    private final SearchCategoryRepository sourceCategoryRepository;
    private final SearchStoreRepository sourceStoreRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public String providerCode() {
        return "postgres";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SearchProductDocument> searchProducts(String query, UUID categoryId, UUID brandId, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isBlank()) {
            return Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }
        return documentRepository.searchActiveDocuments(normalizedQuery, categoryId, brandId,
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<SearchSuggestionDto> predictive(String query, String scope) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        String normalizedScope = normalizeScope(scope);
        Set<SearchSuggestionDto> suggestions = new LinkedHashSet<>();

        if ("all".equals(normalizedScope) || "products".equals(normalizedScope)) {
            documentRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByPopularityScoreDescSourceUpdatedAtDesc(
                            normalizedQuery)
                    .stream()
                    .limit(PRODUCT_LIMIT)
                    .map(document -> new SearchSuggestionDto(document.getName(), "products"))
                    .forEach(suggestions::add);
        }

        if ("all".equals(normalizedScope) || "brands".equals(normalizedScope)) {
            addDistinctSuggestions(
                    suggestions,
                    documentRepository.findTop20ByActiveTrueAndBrandNameContainingIgnoreCaseOrderByBrandNameAscPopularityScoreDesc(
                                    normalizedQuery)
                            .stream()
                            .map(SearchProductDocument::getBrandName)
                            .toList(),
                    "brands",
                    ENTITY_LIMIT
            );
        }

        if ("all".equals(normalizedScope) || "categories".equals(normalizedScope)) {
            addDistinctSuggestions(
                    suggestions,
                    documentRepository.findTop20ByActiveTrueAndCategoryNameContainingIgnoreCaseOrderByCategoryNameAscPopularityScoreDesc(
                                    normalizedQuery)
                            .stream()
                            .map(SearchProductDocument::getCategoryName)
                            .toList(),
                    "categories",
                    ENTITY_LIMIT
            );
        }

        // Store suggestions remain a read-through compatibility path until store discovery is projected.
        if ("all".equals(normalizedScope) || "stores".equals(normalizedScope)) {
            sourceStoreRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(normalizedQuery).stream()
                    .limit(ENTITY_LIMIT)
                    .map(store -> new SearchSuggestionDto(store.getName(), "stores"))
                    .forEach(suggestions::add);
        }

        if (suggestions.size() >= MAX_RESULTS) {
            return new ArrayList<>(suggestions).subList(0, MAX_RESULTS);
        }
        return new ArrayList<>(suggestions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<TrendTagDto> trendTags() {
        List<SearchProductDocument> products =
                documentRepository.findTop20ByActiveTrueAndTrendingTrueOrderByPopularityScoreDescSourceUpdatedAtDesc();
        if (products.isEmpty()) {
            return List.of(
                    new TrendTagDto("Featured", 100),
                    new TrendTagDto("Popular", 90),
                    new TrendTagDto("New", 80)
            );
        }

        Map<String, Integer> scoreByTag = new LinkedHashMap<>();
        for (SearchProductDocument product : products) {
            String tag = normalizeTag(product.getCategoryName());
            scoreByTag.merge(tag, Math.max(1, product.getPopularityScore()), Integer::sum);
        }

        return scoreByTag.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> new TrendTagDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<SearchProductDocument> upsertDocuments(List<SearchProductDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documentRepository.saveAll(documents);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int rebuildProductIndex() {
        List<SearchProduct> sourceProducts = sourceProductRepository.findAllByOrderByUpdatedAtDesc();
        Map<UUID, String> categoryNames = categoryNameMap(
                sourceProducts.stream().map(SearchProduct::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet())
        );
        Map<UUID, String> brandNames = brandNameMap(
                sourceProducts.stream().map(SearchProduct::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet())
        );

        List<SearchProductDocument> documents = sourceProducts.stream()
                .map(product -> toDocument(product, categoryNames, brandNames))
                .toList();

        documentRepository.deleteAllInBatch();
        documentRepository.saveAll(documents);
        return documents.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteDocument(UUID productId) {
        documentRepository.deleteById(productId);
    }

    /**
     * Maps one canonical source product into the search projection shape.
     *
     * @param product source product
     * @param categoryNames category name map
     * @param brandNames brand name map
     * @return search document
     */
    private SearchProductDocument toDocument(
            SearchProduct product,
            Map<UUID, String> categoryNames,
            Map<UUID, String> brandNames
    ) {
        SearchProductDocument document = new SearchProductDocument();
        document.setProductId(product.getId());
        document.setProductCode(product.getProductCode());
        document.setName(product.getName());
        document.setSlug(product.getSlug());
        document.setCategoryId(product.getCategoryId());
        document.setCategoryName(categoryNames.get(product.getCategoryId()));
        document.setBrandId(product.getBrandId());
        document.setBrandName(brandNames.get(product.getBrandId()));
        document.setShortDescription(product.getShortDescription());
        document.setActive(product.isActive());
        document.setTrending(product.isTrending());
        document.setPopularityScore(product.getPopularityScore());
        document.setAverageRating(product.getAverageRating());
        document.setReviewCount(product.getReviewCount());
        document.setSourceUpdatedAt(product.getUpdatedAt());
        return document;
    }

    /**
     * Adds deduplicated string suggestions to the shared suggestion set.
     *
     * @param target target suggestion set
     * @param values raw source values
     * @param scope suggestion scope code
     * @param limit max values to add
     */
    private void addDistinctSuggestions(
            Set<SearchSuggestionDto> target,
            Collection<String> values,
            String scope,
            int limit
    ) {
        if (values == null || values.isEmpty()) {
            return;
        }
        values.stream()
                .map(this::normalizeOptionalText)
                .filter(Objects::nonNull)
                .distinct()
                .limit(limit)
                .map(value -> new SearchSuggestionDto(value, scope))
                .forEach(target::add);
    }

    /**
     * Loads category names for the supplied identifiers.
     *
     * @param ids category identifiers
     * @return category name map
     */
    private Map<UUID, String> categoryNameMap(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return sourceCategoryRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(SearchCategory::getId, SearchCategory::getName, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * Loads brand names for the supplied identifiers.
     *
     * @param ids brand identifiers
     * @return brand name map
     */
    private Map<UUID, String> brandNameMap(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return sourceBrandRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(SearchBrand::getId, SearchBrand::getName, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * Normalizes user query text.
     *
     * @param query source query
     * @return normalized query
     */
    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes suggestion scope strings.
     *
     * @param scope source scope
     * @return normalized scope
     */
    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "all";
        }
        return scope.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes one optional text field.
     *
     * @param value source text
     * @return trimmed text or {@code null}
     */
    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Resolves a safe trend-tag label.
     *
     * @param value category name
     * @return tag label
     */
    private String normalizeTag(String value) {
        String normalized = normalizeOptionalText(value);
        return normalized == null ? "Trending" : normalized;
    }
}
