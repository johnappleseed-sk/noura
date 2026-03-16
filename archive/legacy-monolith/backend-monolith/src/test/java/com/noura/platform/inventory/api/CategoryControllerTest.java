package com.noura.platform.inventory.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.inventory.config.InventorySecurityConfig;
import com.noura.platform.inventory.dto.category.CategoryFilter;
import com.noura.platform.inventory.dto.category.CategoryRequest;
import com.noura.platform.inventory.dto.category.CategoryResponse;
import com.noura.platform.inventory.service.CategoryService;
import com.noura.platform.testsupport.inventory.api.InventoryWebMvcSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({CategoryController.class, InventorySecurityConfig.class, InventoryExceptionHandler.class, InventoryWebMvcSecurityTestConfig.class})
@ActiveProfiles("inventory-webmvc-test")
@TestPropertySource(properties = {
        "inventory.api.base-path=/api/inventory/v1",
        "inventory.security.dev-header-auth-enabled=false",
        "inventory.security.jwt.secret=12345678901234567890123456789012"
})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listCategoriesReturnsPagedEnvelope() throws Exception {
        CategoryResponse category = new CategoryResponse(
                "cat-1",
                null,
                "APPAREL",
                "Apparel",
                "All apparel",
                0,
                0,
                true,
                Instant.parse("2026-03-08T00:00:00Z"),
                Instant.parse("2026-03-08T00:00:00Z")
        );
        when(categoryService.listCategories(any(CategoryFilter.class), any()))
                .thenReturn(new PageImpl<>(List.of(category), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/inventory/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].categoryCode").value("APPAREL"));
    }

    @Test
    void createCategoryRequiresAuthentication() throws Exception {
        CategoryRequest request = new CategoryRequest(null, "APPAREL", "Apparel", "All apparel", 0, true);

        mockMvc.perform(post("/api/inventory/v1/categories")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategoryReturnsCreated() throws Exception {
        CategoryRequest request = new CategoryRequest(null, "APPAREL", "Apparel", "All apparel", 0, true);
        CategoryResponse response = new CategoryResponse(
                "cat-1",
                null,
                "APPAREL",
                "Apparel",
                "All apparel",
                0,
                0,
                true,
                Instant.parse("2026-03-08T00:00:00Z"),
                Instant.parse("2026-03-08T00:00:00Z")
        );
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/inventory/v1/categories")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("cat-1"));
    }
}
