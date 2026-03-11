package com.noura.platform.security;

import com.noura.platform.config.AppProperties;
import com.noura.platform.config.RateLimitFilter;
import com.noura.platform.config.RequestCorrelationFilter;
import com.noura.platform.config.SecurityConfig;
import com.noura.platform.controller.ProductController;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.dto.catalog.AttributeDto;
import com.noura.platform.dto.catalog.AttributeRequest;
import com.noura.platform.dto.catalog.AttributeSetDto;
import com.noura.platform.dto.catalog.AttributeSetRequest;
import com.noura.platform.dto.catalog.CategoryAnalyticsDto;
import com.noura.platform.dto.catalog.CategoryChangeRequestDto;
import com.noura.platform.dto.catalog.CategoryChangeReviewRequest;
import com.noura.platform.dto.catalog.CategoryChangeSubmitRequest;
import com.noura.platform.dto.catalog.CategoryDto;
import com.noura.platform.dto.catalog.CategoryRequest;
import com.noura.platform.dto.catalog.CategorySuggestionRequest;
import com.noura.platform.dto.catalog.CategorySuggestionResponse;
import com.noura.platform.dto.catalog.CategoryTranslationDto;
import com.noura.platform.dto.catalog.CategoryTranslationRequest;
import com.noura.platform.dto.catalog.CategoryTreeDto;
import com.noura.platform.dto.catalog.CategoryUpdateRequest;
import com.noura.platform.dto.catalog.ChannelCategoryMappingDto;
import com.noura.platform.dto.catalog.ChannelCategoryMappingRequest;
import com.noura.platform.dto.product.ProductDto;
import com.noura.platform.dto.product.ProductFilterRequest;
import com.noura.platform.dto.product.ProductInventoryDto;
import com.noura.platform.dto.product.ProductInventoryRequest;
import com.noura.platform.dto.product.ProductMediaDto;
import com.noura.platform.dto.product.ProductMediaRequest;
import com.noura.platform.dto.product.ProductPatchRequest;
import com.noura.platform.dto.product.ProductRequest;
import com.noura.platform.dto.product.ProductReviewDto;
import com.noura.platform.dto.product.ProductReviewRequest;
import com.noura.platform.dto.product.ProductVariantDto;
import com.noura.platform.dto.product.ProductVariantRequest;
import com.noura.platform.dto.product.TrendTagDto;
import com.noura.platform.service.UnifiedCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.noura.platform.controller.ProductController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        RequestCorrelationFilter.class,
        RateLimitFilter.class,
        ProductControllerSecurityIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "app.api.version-prefix=/api/v1",
        "app.jwt.secret=0123456789abcdef0123456789abcdef",
        "app.jwt.issuer=noura-test",
        "spring.main.allow-bean-definition-overriding=true"
})
class ProductControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(ProductController.class)
    static class TestApplication {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        UnifiedCatalogService unifiedCatalogService() {
            return new UnifiedCatalogService() {
                @Override
                public Page<ProductDto> listProducts(ProductFilterRequest filter, Pageable pageable) {
                    return Page.empty(pageable);
                }

                @Override
                public List<TrendTagDto> trendTags() {
                    return List.of();
                }

                @Override
                public ProductDto getProduct(UUID productId) {
                    return null;
                }

                @Override
                @PreAuthorize("hasRole('ADMIN')")
                public ProductDto createProduct(ProductRequest request) {
                    return null;
                }

                @Override
                public ProductDto updateProduct(UUID productId, ProductRequest request) {
                    return null;
                }

                @Override
                public ProductDto patchProduct(UUID productId, ProductPatchRequest request) {
                    return null;
                }

                @Override
                public void deleteProduct(UUID productId) {
                }

                @Override
                public List<ProductReviewDto> reviews(UUID productId) {
                    return List.of();
                }

                @Override
                public ProductReviewDto addReview(UUID productId, ProductReviewRequest request) {
                    return null;
                }

                @Override
                public ProductVariantDto addVariant(UUID productId, ProductVariantRequest request) {
                    return null;
                }

                @Override
                public List<ProductVariantDto> listVariants(UUID productId) {
                    return List.of();
                }

                @Override
                public ProductVariantDto updateVariant(UUID variantId, ProductVariantRequest request) {
                    return null;
                }

                @Override
                public ProductMediaDto addMedia(UUID productId, ProductMediaRequest request) {
                    return null;
                }

                @Override
                public ProductInventoryDto upsertInventory(UUID productId, ProductInventoryRequest request) {
                    return null;
                }

                @Override
                public List<ProductInventoryDto> inventories(UUID productId) {
                    return List.of();
                }

                @Override
                public List<ProductDto> relatedProducts(UUID productId) {
                    return List.of();
                }

                @Override
                public List<ProductDto> frequentlyBoughtTogether(UUID productId) {
                    return List.of();
                }

                @Override
                public CategoryDto createCategory(CategoryRequest request) {
                    return null;
                }

                @Override
                public List<CategoryTreeDto> categoryTree(String locale) {
                    return List.of();
                }

                @Override
                public CategoryDto updateCategory(UUID categoryId, CategoryUpdateRequest request) {
                    return null;
                }

                @Override
                public AttributeDto createAttribute(AttributeRequest request) {
                    return null;
                }

                @Override
                public AttributeSetDto createAttributeSet(AttributeSetRequest request) {
                    return null;
                }

                @Override
                public CategoryTranslationDto upsertCategoryTranslation(UUID categoryId, String locale, CategoryTranslationRequest request) {
                    return null;
                }

                @Override
                public List<CategoryTranslationDto> categoryTranslations(UUID categoryId) {
                    return List.of();
                }

                @Override
                public ChannelCategoryMappingDto createChannelCategoryMapping(ChannelCategoryMappingRequest request) {
                    return null;
                }

                @Override
                public List<ChannelCategoryMappingDto> categoryChannelMappings(UUID categoryId) {
                    return List.of();
                }

                @Override
                public CategoryChangeRequestDto submitCategoryChangeRequest(CategoryChangeSubmitRequest request) {
                    return null;
                }

                @Override
                public List<CategoryChangeRequestDto> categoryChangeRequests(com.noura.platform.domain.enums.CategoryChangeRequestStatus status) {
                    return List.of();
                }

                @Override
                public CategoryChangeRequestDto approveCategoryChangeRequest(UUID requestId, CategoryChangeReviewRequest request) {
                    return null;
                }

                @Override
                public CategoryChangeRequestDto rejectCategoryChangeRequest(UUID requestId, CategoryChangeReviewRequest request) {
                    return null;
                }

                @Override
                public CategorySuggestionResponse suggestCategory(CategorySuggestionRequest request) {
                    return null;
                }

                @Override
                public List<CategoryAnalyticsDto> categoryAnalytics(Instant from, Instant to) {
                    return List.of();
                }

                @Override
                public Page<com.noura.platform.commerce.api.v1.dto.product.ApiProductDto> listCommerceProducts(String q, Long categoryId, Boolean active, Boolean lowStock, Pageable pageable) {
                    return Page.empty(pageable);
                }

                @Override
                public com.noura.platform.commerce.api.v1.dto.product.ApiProductDto getCommerceProductById(Long id) {
                    return null;
                }

                @Override
                public com.noura.platform.commerce.api.v1.dto.product.ApiProductDto createCommerceProduct(com.noura.platform.commerce.api.v1.dto.product.ProductCreateRequest request) {
                    return null;
                }

                @Override
                public com.noura.platform.commerce.api.v1.dto.product.ApiProductDto updateCommerceProduct(Long id, com.noura.platform.commerce.api.v1.dto.product.ProductUpdateRequest request) {
                    return null;
                }

                @Override
                public List<com.noura.platform.commerce.api.v1.dto.product.ApiProductUnitDto> listCommerceProductUnits(Long productId) {
                    return List.of();
                }

                @Override
                public com.noura.platform.commerce.api.v1.dto.product.ApiProductUnitDto createCommerceProductUnit(Long productId, com.noura.platform.commerce.api.v1.dto.product.ProductUnitUpsertRequest request) {
                    return null;
                }

                @Override
                public com.noura.platform.commerce.api.v1.dto.product.ApiProductUnitDto updateCommerceProductUnit(Long productId, Long unitId, com.noura.platform.commerce.api.v1.dto.product.ProductUnitUpsertRequest request) {
                    return null;
                }

                @Override
                public void deleteCommerceProductUnit(Long productId, Long unitId) {
                }

                @Override
                public List<com.noura.platform.commerce.catalog.web.StorefrontCategoryDto> listStorefrontCategories() {
                    return List.of();
                }

                @Override
                public Page<com.noura.platform.commerce.catalog.web.StorefrontProductCardDto> listStorefrontProducts(String q, Long categoryId, Pageable pageable) {
                    return Page.empty(pageable);
                }

                @Override
                public com.noura.platform.commerce.catalog.web.StorefrontProductDetailDto getStorefrontProduct(Long productId) {
                    return null;
                }

                @Override
                public com.noura.platform.commerce.api.v1.dto.inventory.StockAvailabilityDto getStorefrontProductAvailability(Long productId) {
                    return null;
                }
            };
        }
    }

    @Test
    void createProduct_shouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreatePayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"))
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void createProduct_shouldRejectNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(user("customer@noura.test").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreatePayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void createProduct_shouldRejectJwtWithInvalidSignature() throws Exception {
        String invalidSignatureToken = tokenSignedWithDifferentSecret();

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidSignatureToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreatePayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    private String validCreatePayload() {
        return """
                {
                  "name": "Noura Test Product",
                  "category": "Electronics",
                  "brand": "Noura",
                  "price": 99.99
                }
                """;
    }

    private String tokenSignedWithDifferentSecret() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("abcdef0123456789abcdef0123456789");
        properties.getJwt().setIssuer("noura-test");
        properties.getJwt().setAccessTokenValidityMinutes(30);
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        return provider.generateAccessToken(
                UUID.randomUUID(),
                "customer@noura.test",
                Set.of(RoleType.CUSTOMER)
        );
    }

}
