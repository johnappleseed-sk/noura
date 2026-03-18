package com.noura.pricing.controller;

import com.noura.pricing.dto.price.PriceResolutionResponse;
import com.noura.pricing.exception.ApiExceptionHandler;
import com.noura.pricing.integration.client.CatalogVariantLookupClient;
import com.noura.pricing.repository.LegacyPriceListRepository;
import com.noura.pricing.service.ProductPricingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link PricingLegacyCompatibilityController}.
 */
@WebMvcTest(controllers = PricingLegacyCompatibilityController.class)
@Import({
        ApiExceptionHandler.class,
        PricingLegacyCompatibilityControllerTest.TestConfiguration.class
})
class PricingLegacyCompatibilityControllerTest {

    private static final UUID RESOLVED_PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductPricingService productPricingService;

    @MockBean
    private LegacyPriceListRepository legacyPriceListRepository;

    /**
     * Verifies legacy variant quote endpoint returns the admin-web-compatible payload shape.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void quoteLegacyVariantPriceReturnsCompatibilityPayload() throws Exception {
        UUID variantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID sourcePriceId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        when(productPricingService.resolveProductPrice(eq(RESOLVED_PRODUCT_ID), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new PriceResolutionResponse(
                        RESOLVED_PRODUCT_ID,
                        "USD",
                        new BigDecimal("42.0000"),
                        null,
                        new BigDecimal("39.0000"),
                        sourcePriceId,
                        null,
                        null,
                        null,
                        null,
                        0,
                        Instant.parse("2026-03-18T00:00:00Z")
                ));

        mockMvc.perform(get("/api/v1/prices/variants/{variantId}", variantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.variantId").value(variantId.toString()))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.baseAmount").value(42))
                .andExpect(jsonPath("$.data.finalAmount").value(39))
                .andExpect(jsonPath("$.data.appliedPromotionIds").isArray());
    }

    /**
     * Supplies a concrete lookup bean instead of a Mockito inline mock so the test remains
     * stable on the current Java 25 toolchain used in CI and local validation.
     */
    static class TestConfiguration {

        @Bean
        CatalogVariantLookupClient catalogVariantLookupClient() {
            return new CatalogVariantLookupClient(org.springframework.web.client.RestClient.builder(), "http://localhost") {
                @Override
                public UUID resolveProductId(UUID variantId) {
                    return RESOLVED_PRODUCT_ID;
                }
            };
        }
    }
}
