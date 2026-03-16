package com.noura.platform.service;

import com.noura.platform.config.AppProperties;
import com.noura.platform.repository.ProductGeneratorBridgeRepository;
import com.noura.platform.repository.ProductGeneratorMirrorJobRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.service.impl.ProductEnrichmentServiceImpl;
import com.noura.platform.service.impl.productgen.ProductCodeImageService;
import com.noura.platform.service.impl.productgen.ProductDescriptionGenerationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("method-security-test")
@ContextConfiguration(classes = ProductEnrichmentServiceMethodSecurityTest.Config.class)
class ProductEnrichmentServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private ProductEnrichmentService productEnrichmentService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void searchExistingProducts_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> productEnrichmentService.searchExistingProducts("phone"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void generateMissingFields_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> productEnrichmentService.generateMissingFields(UUID.randomUUID()));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Config {

        @Bean ProductRepository productRepository() { return mock(ProductRepository.class); }
        @Bean ProductVariantRepository productVariantRepository() { return mock(ProductVariantRepository.class); }
        @Bean ProductGeneratorBridgeRepository productGeneratorBridgeRepository() { return mock(ProductGeneratorBridgeRepository.class); }
        @Bean ProductGeneratorMirrorJobRepository productGeneratorMirrorJobRepository() { return mock(ProductGeneratorMirrorJobRepository.class); }
        @Bean ProductDescriptionGenerationService productDescriptionGenerationService() { return mock(ProductDescriptionGenerationService.class); }
        @Bean ProductCodeImageService productCodeImageService() { return mock(ProductCodeImageService.class); }
        @Bean AppProperties appProperties() { return new AppProperties(); }

        @Bean
        ProductEnrichmentService productEnrichmentService(
                ProductRepository productRepository,
                ProductVariantRepository productVariantRepository,
                ProductGeneratorBridgeRepository productGeneratorBridgeRepository,
                ProductGeneratorMirrorJobRepository productGeneratorMirrorJobRepository,
                ProductDescriptionGenerationService productDescriptionGenerationService,
                ProductCodeImageService productCodeImageService,
                AppProperties appProperties
        ) {
            return new ProductEnrichmentServiceImpl(
                    productRepository,
                    productVariantRepository,
                    productGeneratorBridgeRepository,
                    productGeneratorMirrorJobRepository,
                    productDescriptionGenerationService,
                    productCodeImageService,
                    appProperties
            );
        }
    }
}
