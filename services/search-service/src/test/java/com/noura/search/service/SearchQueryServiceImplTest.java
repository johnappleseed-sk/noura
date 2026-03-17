package com.noura.search.service;

import com.noura.search.domain.entity.SearchProductDocument;
import com.noura.search.dto.ProductSearchHitDto;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import com.noura.search.provider.ProductSearchIndexProvider;
import com.noura.search.service.impl.SearchQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SearchQueryServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class SearchQueryServiceImplTest {

    @Mock
    private ProductSearchIndexProvider productSearchIndexProvider;

    private SearchQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchQueryServiceImpl(productSearchIndexProvider);
    }

    /**
     * Verifies indexed documents are mapped into the public product-hit contract.
     */
    @Test
    void searchProductsMapsDocumentsToHits() {
        UUID productId = UUID.randomUUID();
        SearchProductDocument document = new SearchProductDocument();
        document.setProductId(productId);
        document.setProductCode("SKU-1001");
        document.setName("Travel Mug");
        document.setSlug("travel-mug");
        document.setAverageRating(4.7);
        document.setReviewCount(12);
        document.setTrending(true);
        document.setPopularityScore(88);
        document.setSourceUpdatedAt(Instant.now());

        when(productSearchIndexProvider.searchProducts("mug", null, null, 0, 20))
                .thenReturn(new PageImpl<>(List.of(document), PageRequest.of(0, 20), 1));

        ProductSearchHitDto hit = service.searchProducts("mug", null, null, 0, 20).getContent().getFirst();

        assertThat(hit.productId()).isEqualTo(productId);
        assertThat(hit.productCode()).isEqualTo("SKU-1001");
        assertThat(hit.name()).isEqualTo("Travel Mug");
        assertThat(hit.reviewCount()).isEqualTo(12);
        assertThat(hit.trending()).isTrue();
    }

    /**
     * Verifies predictive suggestions delegate to the provider abstraction unchanged.
     */
    @Test
    void predictiveDelegatesToProvider() {
        when(productSearchIndexProvider.predictive("lap", "all"))
                .thenReturn(List.of(new SearchSuggestionDto("Laptop", "products")));

        List<SearchSuggestionDto> result = service.predictive("lap", "all");

        assertThat(result).containsExactly(new SearchSuggestionDto("Laptop", "products"));
        verify(productSearchIndexProvider).predictive("lap", "all");
    }

    /**
     * Verifies trend tags delegate to the provider abstraction unchanged.
     */
    @Test
    void trendTagsDelegateToProvider() {
        when(productSearchIndexProvider.trendTags())
                .thenReturn(List.of(new TrendTagDto("Electronics", 42)));

        List<TrendTagDto> result = service.trendTags();

        assertThat(result).containsExactly(new TrendTagDto("Electronics", 42));
        verify(productSearchIndexProvider).trendTags();
    }
}
