package com.noura.search.service;

import com.noura.search.domain.entity.SearchBrand;
import com.noura.search.domain.entity.SearchCategory;
import com.noura.search.domain.entity.SearchProduct;
import com.noura.search.domain.entity.SearchStore;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import com.noura.search.repository.SearchBrandRepository;
import com.noura.search.repository.SearchCategoryRepository;
import com.noura.search.repository.SearchProductRepository;
import com.noura.search.repository.SearchStoreRepository;
import com.noura.search.service.impl.SearchQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchQueryServiceImplTest {

    @Mock
    private SearchProductRepository productRepository;
    @Mock
    private SearchStoreRepository storeRepository;
    @Mock
    private SearchBrandRepository brandRepository;
    @Mock
    private SearchCategoryRepository categoryRepository;

    private SearchQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchQueryServiceImpl(productRepository, storeRepository, brandRepository, categoryRepository);
    }

    @Test
    void predictiveReturnsEmptyWhenQueryIsBlank() {
        List<SearchSuggestionDto> result = service.predictive("   ", "all");
        assertThat(result).isEmpty();
        verifyNoInteractions(productRepository, storeRepository, brandRepository, categoryRepository);
    }

    @Test
    void predictiveReturnsProductSuggestionsForProductScope() {
        SearchProduct product = new SearchProduct();
        product.setId(UUID.randomUUID());
        product.setName("Galaxy S25");
        product.setUpdatedAt(Instant.now());
        product.setActive(true);
        when(productRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc("galaxy"))
                .thenReturn(List.of(product));

        List<SearchSuggestionDto> result = service.predictive("Galaxy", "products");

        assertThat(result).containsExactly(new SearchSuggestionDto("Galaxy S25", "products"));
        verify(storeRepository, never()).findTop10ByNameContainingIgnoreCaseOrderByNameAsc("galaxy");
        verify(brandRepository, never()).findTop10ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc("galaxy");
    }

    @Test
    void predictiveReturnsMixedSuggestionsForAllScope() {
        SearchProduct product = new SearchProduct();
        product.setName("Air Purifier");
        product.setUpdatedAt(Instant.now());
        product.setActive(true);

        SearchStore store = new SearchStore();
        store.setName("Central Store");

        SearchBrand brand = new SearchBrand();
        brand.setName("NouraHome");
        brand.setActive(true);

        when(productRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc("air"))
                .thenReturn(List.of(product));
        when(storeRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc("air"))
                .thenReturn(List.of(store));
        when(brandRepository.findTop10ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc("air"))
                .thenReturn(List.of(brand));

        List<SearchSuggestionDto> result = service.predictive("air", "all");

        assertThat(result).containsExactly(
                new SearchSuggestionDto("Air Purifier", "products"),
                new SearchSuggestionDto("Central Store", "stores"),
                new SearchSuggestionDto("NouraHome", "brands")
        );
    }

    @Test
    void trendTagsReturnsFallbackWhenNoTrendingProducts() {
        when(productRepository.findTop20ByActiveTrueAndTrendingTrueOrderByPopularityScoreDesc()).thenReturn(List.of());

        List<TrendTagDto> result = service.trendTags();

        assertThat(result).containsExactly(
                new TrendTagDto("Featured", 100),
                new TrendTagDto("Popular", 90),
                new TrendTagDto("New", 80)
        );
    }

    @Test
    void trendTagsAggregatesByCategory() {
        UUID categoryId = UUID.randomUUID();
        SearchProduct p1 = new SearchProduct();
        p1.setCategoryId(categoryId);
        p1.setPopularityScore(7);
        p1.setTrending(true);
        p1.setActive(true);

        SearchProduct p2 = new SearchProduct();
        p2.setCategoryId(categoryId);
        p2.setPopularityScore(3);
        p2.setTrending(true);
        p2.setActive(true);

        SearchCategory category = new SearchCategory();
        category.setId(categoryId);
        category.setName("Electronics");

        when(productRepository.findTop20ByActiveTrueAndTrendingTrueOrderByPopularityScoreDesc())
                .thenReturn(List.of(p1, p2));
        when(categoryRepository.findByIdIn(anyCollection())).thenReturn(List.of(category));

        List<TrendTagDto> result = service.trendTags();

        assertThat(result).containsExactly(new TrendTagDto("Electronics", 10));
    }
}
