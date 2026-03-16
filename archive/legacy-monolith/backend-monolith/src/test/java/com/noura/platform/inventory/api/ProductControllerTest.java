package com.noura.platform.inventory.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.inventory.config.InventorySecurityConfig;
import com.noura.platform.inventory.dto.category.CategorySummaryResponse;
import com.noura.platform.inventory.dto.product.ProductRequest;
import com.noura.platform.inventory.dto.product.ProductResponse;
import com.noura.platform.inventory.service.ProductService;
import com.noura.platform.testsupport.inventory.api.InventoryWebMvcSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({ProductController.class, InventorySecurityConfig.class, InventoryExceptionHandler.class, InventoryWebMvcSecurityTestConfig.class})
@ActiveProfiles("inventory-webmvc-test")
@TestPropertySource(properties = {
        "inventory.api.base-path=/api/inventory/v1",
        "inventory.security.dev-header-auth-enabled=false",
        "inventory.security.jwt.secret=12345678901234567890123456789012"
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void createProductIsForbiddenForWarehouseManager() throws Exception {
        ProductRequest request = new ProductRequest(
                "SKU-1",
                "Running Shoe",
                "Lightweight shoe",
                "ACTIVE",
                new BigDecimal("99.90"),
                "USD",
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                null,
                true,
                List.of("cat-1"),
                "cat-1"
        );

        mockMvc.perform(post("/api/inventory/v1/products")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProductReturnsCreatedProduct() throws Exception {
        ProductRequest request = new ProductRequest(
                "SKU-1",
                "Running Shoe",
                "Lightweight shoe",
                "ACTIVE",
                new BigDecimal("99.90"),
                "USD",
                null,
                null,
                null,
                null,
                false,
                false,
                "1234567890",
                null,
                true,
                List.of("cat-1"),
                "cat-1"
        );
        ProductResponse response = new ProductResponse(
                "prod-1",
                "SKU-1",
                "Running Shoe",
                "Lightweight shoe",
                "ACTIVE",
                new BigDecimal("99.90"),
                "USD",
                null,
                null,
                null,
                null,
                false,
                false,
                "1234567890",
                null,
                true,
                new CategorySummaryResponse("cat-1", "SHOES", "Shoes", 1, true),
                List.of(new CategorySummaryResponse("cat-1", "SHOES", "Shoes", 1, true)),
                Instant.parse("2026-03-08T00:00:00Z"),
                Instant.parse("2026-03-08T00:00:00Z")
        );
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/inventory/v1/products")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku").value("SKU-1"))
                .andExpect(jsonPath("$.data.primaryCategory.categoryCode").value("SHOES"));
    }
}
