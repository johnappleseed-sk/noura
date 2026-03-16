package com.noura.search.controller;

import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import com.noura.search.exception.ApiExceptionHandler;
import com.noura.search.service.SearchQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SearchPublicController.class)
@Import(ApiExceptionHandler.class)
class SearchPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchQueryService searchQueryService;

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

    @Test
    void trendTagsReturnsApiEnvelope() throws Exception {
        when(searchQueryService.trendTags()).thenReturn(List.of(new TrendTagDto("Electronics", 42)));

        mockMvc.perform(get("/api/v1/search/trend-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Trend tags"))
                .andExpect(jsonPath("$.data[0].value").value("Electronics"))
                .andExpect(jsonPath("$.data[0].score").value(42));
    }
}
