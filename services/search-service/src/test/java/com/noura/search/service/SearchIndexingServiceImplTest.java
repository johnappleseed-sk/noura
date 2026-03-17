package com.noura.search.service;

import com.noura.search.domain.entity.SearchProductDocument;
import com.noura.search.dto.ProductSearchDocumentBatchUpsertRequest;
import com.noura.search.dto.ProductSearchDocumentUpsertRequest;
import com.noura.search.dto.SearchIndexRebuildResponse;
import com.noura.search.dto.SearchProductDocumentResponse;
import com.noura.search.provider.ProductSearchIndexProvider;
import com.noura.search.service.impl.SearchIndexingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SearchIndexingServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class SearchIndexingServiceImplTest {

    @Mock
    private ProductSearchIndexProvider productSearchIndexProvider;

    @Captor
    private ArgumentCaptor<List<SearchProductDocument>> documentsCaptor;

    private SearchIndexingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchIndexingServiceImpl(productSearchIndexProvider);
    }

    /**
     * Verifies internal upsert requests are mapped into search documents.
     */
    @Test
    void upsertProductDocumentsMapsRequestsToProjectionDocuments() {
        UUID productId = UUID.randomUUID();
        SearchProductDocument saved = new SearchProductDocument();
        saved.setProductId(productId);
        saved.setName("Travel Mug");
        saved.setActive(true);
        saved.setIndexedAt(Instant.parse("2026-03-18T01:00:00Z"));
        saved.setSourceUpdatedAt(Instant.parse("2026-03-18T00:30:00Z"));

        when(productSearchIndexProvider.upsertDocuments(anyList())).thenReturn(List.of(saved));

        ProductSearchDocumentBatchUpsertRequest request = new ProductSearchDocumentBatchUpsertRequest(List.of(
                new ProductSearchDocumentUpsertRequest(
                        productId,
                        "SKU-1001",
                        "Travel Mug",
                        "travel-mug",
                        null,
                        "Drinkware",
                        null,
                        "NouraHome",
                        "Insulated mug",
                        true,
                        true,
                        88,
                        4.7,
                        12,
                        Instant.parse("2026-03-18T00:30:00Z")
                )
        ));

        List<SearchProductDocumentResponse> response = service.upsertProductDocuments(request);

        verify(productSearchIndexProvider).upsertDocuments(documentsCaptor.capture());
        SearchProductDocument indexed = documentsCaptor.getValue().getFirst();
        assertThat(indexed.getProductId()).isEqualTo(productId);
        assertThat(indexed.getName()).isEqualTo("Travel Mug");
        assertThat(indexed.getPopularityScore()).isEqualTo(88);
        assertThat(response.getFirst().productId()).isEqualTo(productId);
        assertThat(response.getFirst().indexedAt()).isEqualTo(Instant.parse("2026-03-18T01:00:00Z"));
    }

    /**
     * Verifies rebuild responses expose provider code and indexed count.
     */
    @Test
    void rebuildProductIndexReturnsProviderAndCount() {
        when(productSearchIndexProvider.providerCode()).thenReturn("postgres");
        when(productSearchIndexProvider.rebuildProductIndex()).thenReturn(42);

        SearchIndexRebuildResponse response = service.rebuildProductIndex();

        assertThat(response.provider()).isEqualTo("postgres");
        assertThat(response.indexedCount()).isEqualTo(42);
        assertThat(response.rebuiltAt()).isNotNull();
    }
}
