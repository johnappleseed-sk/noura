package com.noura.catalog.service.impl;

import com.noura.catalog.dto.product.SearchSuggestionDto;
import com.noura.catalog.dto.product.TrendTagDto;
import com.noura.catalog.repository.CatalogBrandRepository;
import com.noura.catalog.repository.CatalogProductRepository;
import com.noura.catalog.repository.CatalogStoreRepository;
import com.noura.catalog.service.CatalogQueryService;
import com.noura.catalog.service.CatalogSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogSearchServiceImpl implements CatalogSearchService {

    private static final int MAX_RESULTS = 20;
    private static final int PRODUCT_LIMIT = 10;
    private static final int ENTITY_LIMIT = 5;

    private final CatalogProductRepository productRepository;
    private final CatalogStoreRepository storeRepository;
    private final CatalogBrandRepository brandRepository;
    private final CatalogQueryService catalogQueryService;

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
            brandRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(normalizedQuery).stream()
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
        return catalogQueryService.trendTags();
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
