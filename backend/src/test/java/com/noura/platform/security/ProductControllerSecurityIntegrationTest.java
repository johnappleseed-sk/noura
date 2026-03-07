package com.noura.platform.security;

import com.noura.platform.config.AppProperties;
import com.noura.platform.config.RateLimitFilter;
import com.noura.platform.config.RequestCorrelationFilter;
import com.noura.platform.config.SecurityConfig;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.dto.product.*;
import com.noura.platform.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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
        "app.jwt.issuer=noura-test"
})
class ProductControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        CustomUserDetailsService customUserDetailsService() {
            return new CustomUserDetailsService(null) {
                @Override
                public UserDetails loadUserByUsername(String email) {
                    return User.withUsername(email)
                            .password("n/a")
                            .authorities("ROLE_CUSTOMER")
                            .build();
                }
            };
        }

        @Bean
        @Primary
        ProductService productService() {
            return new ProductService() {
                @Override
                @PreAuthorize("hasRole('ADMIN')")
                public ProductDto createProduct(ProductRequest request) {
                    return new ProductDto(
                            UUID.randomUUID(),
                            request.name(),
                            request.category(),
                            request.brand(),
                            request.price(),
                            request.flashSale(),
                            request.trending(),
                            request.bestSeller(),
                            0.0,
                            0,
                            0,
                            request.shortDescription(),
                            request.longDescription(),
                            request.seoTitle(),
                            request.seoDescription(),
                            request.seoSlug(),
                            null,
                            java.util.Map.of(),
                            null,
                            true,
                            false,
                            List.of(),
                            List.of(),
                            List.of()
                    );
                }

                @Override
                public ProductDto updateProduct(UUID productId, ProductRequest request) {
                    return null;
                }

                @Override
                public void deleteProduct(UUID productId) {
                }

                @Override
                public ProductDto getProduct(UUID productId) {
                    return null;
                }

                @Override
                public Page<ProductDto> listProducts(ProductFilterRequest filter, Pageable pageable) {
                    return Page.empty(pageable);
                }

                @Override
                public List<ProductVariantDto> listVariants(UUID productId) {
                    return List.of();
                }

                @Override
                public ProductVariantDto addVariant(UUID productId, ProductVariantRequest request) {
                    return null;
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
                public ProductReviewDto addReview(UUID productId, ProductReviewRequest request) {
                    return null;
                }

                @Override
                public List<ProductReviewDto> reviews(UUID productId) {
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
                public List<TrendTagDto> trendTags() {
                    return List.of();
                }
            };
        }
    }
}
