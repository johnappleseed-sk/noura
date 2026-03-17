package com.noura.search.provider;

import com.noura.search.domain.entity.SearchBrand;
import com.noura.search.domain.entity.SearchCategory;
import com.noura.search.domain.entity.SearchProduct;
import com.noura.search.domain.entity.SearchProductDocument;
import com.noura.search.domain.entity.SearchStore;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import com.noura.search.repository.SearchBrandRepository;
import com.noura.search.repository.SearchCategoryRepository;
import com.noura.search.repository.SearchProductDocumentRepository;
import com.noura.search.repository.SearchProductRepository;
import com.noura.search.repository.SearchStoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PostgresProductSearchIndexProvider}.
 */
@ExtendWith(MockitoExtension.class)
class PostgresProductSearchIndexProviderTest {

    @Mock
    private SearchProductDocumentRepository documentRepository;
    @Mock
    private SearchProductRepository sourceProductRepository;
    @Mock
    private SearchBrandRepository sourceBrandRepository;
    @Mock
    private SearchCategoryRepository sourceCategoryRepository;
    @Mock
    private SearchStoreRepository sourceStoreRepository;

    @Captor
    private ArgumentCaptor<List<SearchProductDocument>> documentsCaptor;

    private PostgresProductSearchIndexProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PostgresProductSearchIndexProvider(
                documentRepository,
                sourceProductRepository,
                sourceBrandRepository,
                sourceCategoryRepository,
                sourceStoreRepository
        );
    }

    /**
     * Verifies blank predictive queries short-circuit without repository calls.
     */
    @Test
    void predictiveReturnsEmptyWhenQueryIsBlank() {
        List<SearchSuggestionDto> result = provider.predictive("   ", "all");

        assertThat(result).isEmpty();
        verifyNoInteractions(documentRepository, sourceStoreRepository);
    }

    /**
     * Verifies mixed predictive suggestions are assembled from indexed documents and legacy store lookups.
     */
    @Test
    void predictiveReturnsMixedSuggestions() {
        SearchProductDocument product = new SearchProductDocument();
        product.setName("Travel Mug");

        SearchProductDocument brandDoc = new SearchProductDocument();
        brandDoc.setBrandName("NouraHome");

        SearchProductDocument categoryDoc = new SearchProductDocument();
        categoryDoc.setCategoryName("Drinkware");

        SearchStore store = new SearchStore();
        store.setName("Central Store");

        when(documentRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByPopularityScoreDescSourceUpdatedAtDesc("mug"))
                .thenReturn(List.of(product));
        when(documentRepository.findTop20ByActiveTrueAndBrandNameContainingIgnoreCaseOrderByBrandNameAscPopularityScoreDesc("mug"))
                .thenReturn(List.of(brandDoc));
        when(documentRepository.findTop20ByActiveTrueAndCategoryNameContainingIgnoreCaseOrderByCategoryNameAscPopularityScoreDesc("mug"))
                .thenReturn(List.of(categoryDoc));
        when(sourceStoreRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc("mug"))
                .thenReturn(List.of(store));

        List<SearchSuggestionDto> result = provider.predictive("mug", "all");

        assertThat(result).containsExactly(
                new SearchSuggestionDto("Travel Mug", "products"),
                new SearchSuggestionDto("NouraHome", "brands"),
                new SearchSuggestionDto("Drinkware", "categories"),
                new SearchSuggestionDto("Central Store", "stores")
        );
    }

    /**
     * Verifies trend tags fall back when the projection has no trending products yet.
     */
    @Test
    void trendTagsReturnsFallbackWhenProjectionIsEmpty() {
        when(documentRepository.findTop20ByActiveTrueAndTrendingTrueOrderByPopularityScoreDescSourceUpdatedAtDesc())
                .thenReturn(List.of());

        List<TrendTagDto> result = provider.trendTags();

        assertThat(result).containsExactly(
                new TrendTagDto("Featured", 100),
                new TrendTagDto("Popular", 90),
                new TrendTagDto("New", 80)
        );
    }

    /**
     * Verifies rebuild copies canonical source products into search-owned documents.
     */
    @Test
    void rebuildProductIndexCopiesCanonicalProductsIntoProjection() {
        UUID categoryId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();

        SearchProduct product = new SearchProduct();
        product.setId(UUID.randomUUID());
        product.setProductCode("SKU-1001");
        product.setName("Travel Mug");
        product.setSlug("travel-mug");
        product.setCategoryId(categoryId);
        product.setBrandId(brandId);
        product.setShortDescription("Insulated mug");
        product.setActive(true);
        product.setTrending(true);
        product.setPopularityScore(88);
        product.setAverageRating(4.7);
        product.setReviewCount(12);
        product.setUpdatedAt(Instant.parse("2026-03-18T00:30:00Z"));

        SearchCategory category = new SearchCategory();
        category.setId(categoryId);
        category.setName("Drinkware");

        SearchBrand brand = new SearchBrand();
        brand.setId(brandId);
        brand.setName("NouraHome");
        brand.setActive(true);

        when(sourceProductRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(product));
        when(sourceCategoryRepository.findByIdIn(anyCollection())).thenReturn(List.of(category));
        when(sourceBrandRepository.findByIdIn(anyCollection())).thenReturn(List.of(brand));

        int indexedCount = provider.rebuildProductIndex();

        verify(documentRepository).deleteAllInBatch();
        verify(documentRepository).saveAll(documentsCaptor.capture());
        SearchProductDocument saved = documentsCaptor.getValue().getFirst();
        assertThat(indexedCount).isEqualTo(1);
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getCategoryName()).isEqualTo("Drinkware");
        assertThat(saved.getBrandName()).isEqualTo("NouraHome");
        assertThat(saved.getReviewCount()).isEqualTo(12);
    }
}
