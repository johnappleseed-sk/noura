package com.noura.platform.service.impl;

import com.noura.platform.dto.product.SearchSuggestionDto;
import com.noura.platform.search.ProductSearchGateway;
import com.noura.platform.repository.BrandRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final BrandRepository brandRepository;
    private final ProductSearchGateway productSearchGateway;

    /**
     * Executes predictive.
     *
     * @param q The q value.
     * @param scope The scope value.
     * @return A list of matching items.
     */
    @Override
    public List<SearchSuggestionDto> predictive(String q, String scope) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String normalizedScope = scope == null ? "all" : scope.trim().toLowerCase(Locale.ROOT);
        List<SearchSuggestionDto> results = new ArrayList<>();
        if (query.isBlank()) {
            return results;
        }
        if (normalizedScope.equals("all") || normalizedScope.equals("products")) {
            productSearchGateway.searchProductIds(query, 10).stream()
                    .map(productRepository::findById)
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .forEach(product -> results.add(new SearchSuggestionDto(product.getName(), "products")));
        }
        if (normalizedScope.equals("all") || normalizedScope.equals("stores")) {
            storeRepository.findAll().stream()
                    .filter(store -> store.getName().toLowerCase(Locale.ROOT).contains(query))
                    .limit(10)
                    .forEach(store -> results.add(new SearchSuggestionDto(store.getName(), "stores")));
        }
        if (normalizedScope.equals("all") || normalizedScope.equals("brands")) {
            brandRepository.findAll().stream()
                    .filter(brand -> brand.getName().toLowerCase(Locale.ROOT).contains(query))
                    .limit(10)
                    .forEach(brand -> results.add(new SearchSuggestionDto(brand.getName(), "brands")));
        }
        return results.stream().distinct().limit(20).toList();
    }
}
