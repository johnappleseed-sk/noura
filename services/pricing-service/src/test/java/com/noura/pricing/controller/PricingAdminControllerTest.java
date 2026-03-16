package com.noura.pricing.controller;

import com.noura.pricing.exception.ApiExceptionHandler;
import com.noura.pricing.service.ProductPricingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for {@link PricingAdminController}.
 */
@WebMvcTest(controllers = PricingAdminController.class)
@Import(ApiExceptionHandler.class)
class PricingAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductPricingService productPricingService;

    /**
     * Verifies bean-validation errors are returned in standard API error envelope.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void upsertPriceReturnsValidationErrorForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/pricing/v1/admin/prices")
                        .contentType("application/json")
                        .content("""
                                {
                                  "currencyCode": "USD",
                                  "basePrice": 25.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}

