package com.noura.search.controller;

import com.noura.search.domain.entity.SearchProductDocument;
import com.noura.search.dto.ProductSearchHitDto;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import com.noura.search.exception.ApiExceptionHandler;
import com.noura.search.service.SearchQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web MVC tests for {@link SearchPublicController}.
 */
@WebMvcTest(controllers = SearchPublicController.class)
@Import(ApiExceptionHandler.class)
class SearchPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchQueryService searchQueryService;

    /**
     * Verifies product-search queries return a paginated API envelope.
     */
    @Test
    void searchProductsReturnsApiEnvelope() throws Exception {
        ProductSearchHitDto hit = new ProductSearchHitDto(
                UUID.randomUUID(),
                "SKU-1001",
                "Travel Mug",
                "travel-mug",
                null,
                "Drinkware",
                null,
                "NouraHome",
                4.8,
                18,
                true,
                88
        );
        when(searchQueryService.searchProducts("mug", null, null, 0, 20))
                .thenReturn(new PageImpl<>(List.of(hit), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/search/products")
                        .param("query", "mug")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product search results"))
                .andExpect(jsonPath("$.data.content[0].id").value(hit.productId().toString()))
                .andExpect(jsonPath("$.data.content[0].name").value("Travel Mug"))
                .andExpect(jsonPath("$.data.content[0].productCode").value("SKU-1001"))
                .andExpect(jsonPath("$.data.content[0].isTrending").value(true))
                .andExpect(jsonPath("$.data.content[0].merchandisingScore").value(88))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(false))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    /**
     * Verifies predictive suggestions return the standard API envelope.
     */
    @Test
    void predictiveReturnsApiEnvelope() throws Exception {
        when(searchQueryService.predictive("lap", "all"))
                .thenReturn(List.of(new SearchSuggestionDto("Laptop", "products")));

        mockMvc.perform(get("/api/v1/search/predictive")
                        .param("q", "lap")
                        .param("scope", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Predictive search"))
                .andExpect(jsonPath("$.data[0].value").value("Laptop"))
                .andExpect(jsonPath("$.data[0].scope").value("products"));
    }

    /**
     * Verifies trend tags return the standard API envelope.
     */
    @Test
    void trendTagsReturnsApiEnvelope() throws Exception {
        when(searchQueryService.trendTags()).thenReturn(List.of(new TrendTagDto("Electronics", 42)));

        mockMvc.perform(get("/api/v1/search/trend-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Trend tags"))
                .andExpect(jsonPath("$.data[0].value").value("Electronics"))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"))
                .andExpect(jsonPath("$.data[0].tag").value("Electronics"))
                .andExpect(jsonPath("$.data[0].score").value(42));
    }
}
