package com.noura.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.repository.BrandRepository;
import com.noura.platform.repository.CategoryRepository;
import com.noura.platform.repository.ProductDedupeCandidateRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.ProductSubmissionRequestRepository;
import com.noura.platform.repository.ProductSubmissionReviewRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.UserStoreAssignmentRepository;
import com.noura.platform.service.impl.ProductSubmissionServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("method-security-test")
@ContextConfiguration(classes = ProductSubmissionServiceMethodSecurityTest.Config.class)
class ProductSubmissionServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private ProductSubmissionService productSubmissionService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void approve_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> productSubmissionService.approve(java.util.UUID.randomUUID(), null));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Config {
        @Bean ProductSubmissionRequestRepository submissionRepository() { return mock(ProductSubmissionRequestRepository.class); }
        @Bean ProductSubmissionReviewRepository reviewRepository() { return mock(ProductSubmissionReviewRepository.class); }
        @Bean ProductDedupeCandidateRepository dedupeCandidateRepository() { return mock(ProductDedupeCandidateRepository.class); }
        @Bean StoreRepository storeRepository() { return mock(StoreRepository.class); }
        @Bean StoreTenantRepository storeTenantRepository() { return mock(StoreTenantRepository.class); }
        @Bean UserAccountRepository userAccountRepository() { return mock(UserAccountRepository.class); }
        @Bean UserStoreAssignmentRepository userStoreAssignmentRepository() { return mock(UserStoreAssignmentRepository.class); }
        @Bean ProductRepository productRepository() { return mock(ProductRepository.class); }
        @Bean ProductVariantRepository productVariantRepository() { return mock(ProductVariantRepository.class); }
        @Bean BrandRepository brandRepository() { return mock(BrandRepository.class); }
        @Bean CategoryRepository categoryRepository() { return mock(CategoryRepository.class); }
        @Bean ProductService productService() { return mock(ProductService.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }

        @Bean
        ProductSubmissionService productSubmissionService(
                ProductSubmissionRequestRepository submissionRepository,
                ProductSubmissionReviewRepository reviewRepository,
                ProductDedupeCandidateRepository dedupeCandidateRepository,
                StoreRepository storeRepository,
                StoreTenantRepository storeTenantRepository,
                UserAccountRepository userAccountRepository,
                UserStoreAssignmentRepository userStoreAssignmentRepository,
                ProductRepository productRepository,
                ProductVariantRepository productVariantRepository,
                BrandRepository brandRepository,
                CategoryRepository categoryRepository,
                ProductService productService,
                ObjectMapper objectMapper
        ) {
            return new ProductSubmissionServiceImpl(
                    submissionRepository,
                    reviewRepository,
                    dedupeCandidateRepository,
                    storeRepository,
                    storeTenantRepository,
                    userAccountRepository,
                    userStoreAssignmentRepository,
                    productRepository,
                    productVariantRepository,
                    brandRepository,
                    categoryRepository,
                    productService,
                    objectMapper
            );
        }
    }
}
