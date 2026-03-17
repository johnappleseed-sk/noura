package com.noura.search.controller;

import com.noura.search.dto.ProductSearchDocumentBatchUpsertRequest;
import com.noura.search.dto.ProductSearchDocumentUpsertRequest;
import com.noura.search.dto.SearchIndexRebuildResponse;
import com.noura.search.dto.SearchProductDocumentResponse;
import com.noura.search.exception.ApiExceptionHandler;
import com.noura.search.service.SearchIndexingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web MVC tests for {@link InternalSearchIndexController}.
 */
@WebMvcTest(
        controllers = InternalSearchIndexController.class,
        properties = "app.internal.api-key=shared-secret"
)
@Import({ApiExceptionHandler.class, InternalSearchIndexControllerTest.TestConfig.class})
class InternalSearchIndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchIndexingService searchIndexingService;

    /**
     * Verifies internal indexing rejects requests without the shared API key.
     */
    @Test
    void upsertRejectsInvalidInternalApiKey() throws Exception {
        ProductSearchDocumentBatchUpsertRequest request = new ProductSearchDocumentBatchUpsertRequest(List.of(
                new ProductSearchDocumentUpsertRequest(
                        UUID.randomUUID(),
                        "SKU-1001",
                        "Travel Mug",
                        null,
                        null,
                        "Drinkware",
                        null,
                        "NouraHome",
                        null,
                        true,
                        false,
                        10,
                        4.2,
                        3,
                        Instant.parse("2026-03-18T00:30:00Z")
                )
        ));

        mockMvc.perform(post("/internal/search/index/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SEARCH_INDEX_FORBIDDEN"));
    }

    /**
     * Verifies authenticated internal rebuild calls return the standard API envelope.
     */
    @Test
    void rebuildReturnsApiEnvelope() throws Exception {
        when(searchIndexingService.rebuildProductIndex())
                .thenReturn(new SearchIndexRebuildResponse("postgres", 42, Instant.parse("2026-03-18T01:00:00Z")));

        mockMvc.perform(post("/internal/search/index/products/rebuild")
                        .header("X-Internal-Api-Key", "shared-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Search product index rebuilt"))
                .andExpect(jsonPath("$.data.provider").value("postgres"))
                .andExpect(jsonPath("$.data.indexedCount").value(42));
    }

    /**
     * Verifies internal delete calls return success envelopes when authorized.
     */
    @Test
    void deleteReturnsApiEnvelope() throws Exception {
        mockMvc.perform(delete("/internal/search/index/products/{productId}", UUID.randomUUID())
                        .header("X-Internal-Api-Key", "shared-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Search product document deleted"));
    }

    /**
     * Supplies the configuration-properties dependency required by the MVC slice.
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        com.noura.search.config.InternalApiProperties internalApiProperties(
                @Value("${app.internal.api-key}") String apiKey
        ) {
            com.noura.search.config.InternalApiProperties properties =
                    new com.noura.search.config.InternalApiProperties();
            properties.setApiKey(apiKey);
            return properties;
        }
    }
}
