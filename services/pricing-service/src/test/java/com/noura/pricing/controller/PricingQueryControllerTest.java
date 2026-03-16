package com.noura.pricing.controller;

import com.noura.pricing.dto.price.PriceResolutionResponse;
import com.noura.pricing.exception.ApiExceptionHandler;
import com.noura.pricing.service.ProductPricingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for {@link PricingQueryController}.
 */
@WebMvcTest(controllers = PricingQueryController.class)
@Import(ApiExceptionHandler.class)
class PricingQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductPricingService productPricingService;

    /**
     * Verifies product price endpoint returns standard API envelope.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void getPriceByProductReturnsApiEnvelope() throws Exception {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID sourcePriceId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID storeId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        when(productPricingService.resolveProductPrice(
                eq(productId),
                eq("USD"),
                eq(storeId),
                eq("WEB"),
                eq(Instant.parse("2026-03-16T12:00:00Z"))
        )).thenReturn(new PriceResolutionResponse(
                productId,
                "USD",
                new BigDecimal("100.0000"),
                new BigDecimal("120.0000"),
                new BigDecimal("100.0000"),
                sourcePriceId,
                "WEB",
                storeId,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                10,
                Instant.parse("2026-03-16T12:00:00Z")
        ));

        mockMvc.perform(get("/api/pricing/v1/prices/products/{productId}", productId)
                        .param("currencyCode", "USD")
                        .param("storeId", storeId.toString())
                        .param("channelCode", "WEB")
                        .param("at", "2026-03-16T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.effectivePrice").value(100))
                .andExpect(jsonPath("$.data.sourcePriceId").value(sourcePriceId.toString()));
    }
}

