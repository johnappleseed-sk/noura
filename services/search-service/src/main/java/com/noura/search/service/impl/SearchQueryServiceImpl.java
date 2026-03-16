package com.noura.search.service.impl;

import com.noura.search.domain.entity.SearchCategory;
import com.noura.search.domain.entity.SearchProduct;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import com.noura.search.repository.SearchBrandRepository;
import com.noura.search.repository.SearchCategoryRepository;
import com.noura.search.repository.SearchProductRepository;
import com.noura.search.repository.SearchStoreRepository;
import com.noura.search.service.SearchQueryService;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class SearchQueryServiceImpl implements SearchQueryService {

    private static final int MAX_RESULTS = 20;
    private static final int PRODUCT_LIMIT = 10;
    private static final int ENTITY_LIMIT = 5;

    private final SearchProductRepository productRepository;
    private final SearchStoreRepository storeRepository;
    private final SearchBrandRepository brandRepository;
    private final SearchCategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SearchSuggestionDto> predictive(String q, String scope) {
        String normalizedQuery = normalizeQuery(q);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        String normalizedScope = normalizeScope(scope);
        Set<SearchSuggestionDto> suggestions = new LinkedHashSet<>();

        if ("all".equals(normalizedScope) || "products".equals(normalizedScope)) {
            productRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(normalizedQuery).stream()
                    .limit(PRODUCT_LIMIT)
                    .map(product -> new SearchSuggestionDto(product.getName(), "products"))
                    .forEach(suggestions::add);
        }

        if ("all".equals(normalizedScope) || "stores".equals(normalizedScope)) {
            storeRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(normalizedQuery).stream()
                    .limit(ENTITY_LIMIT)
                    .map(store -> new SearchSuggestionDto(store.getName(), "stores"))
                    .forEach(suggestions::add);
        }

        if ("all".equals(normalizedScope) || "brands".equals(normalizedScope)) {
            brandRepository.findTop10ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(normalizedQuery).stream()
                    .limit(ENTITY_LIMIT)
                    .map(brand -> new SearchSuggestionDto(brand.getName(), "brands"))
                    .forEach(suggestions::add);
        }

        if (suggestions.size() >= MAX_RESULTS) {
            return new ArrayList<>(suggestions).subList(0, MAX_RESULTS);
        }
        return new ArrayList<>(suggestions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrendTagDto> trendTags() {
        List<SearchProduct> products = productRepository.findTop20ByActiveTrueAndTrendingTrueOrderByPopularityScoreDesc();
        if (products.isEmpty()) {
            return List.of(
                    new TrendTagDto("Featured", 100),
                    new TrendTagDto("Popular", 90),
                    new TrendTagDto("New", 80)
            );
        }

        Map<UUID, String> categoryNames = categoryNameMap(
                products.stream().map(SearchProduct::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet())
        );

        Map<String, Integer> scoreByTag = new LinkedHashMap<>();
        for (SearchProduct product : products) {
            String tag = categoryNames.get(product.getCategoryId());
            if (tag == null || tag.isBlank()) {
                tag = "Trending";
            }
            scoreByTag.merge(tag, Math.max(1, product.getPopularityScore()), Integer::sum);
        }

        return scoreByTag.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> new TrendTagDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<UUID, String> categoryNameMap(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(SearchCategory::getId, SearchCategory::getName, (left, right) -> left, LinkedHashMap::new));
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "all";
        }
        return scope.trim().toLowerCase(Locale.ROOT);
    }
}
