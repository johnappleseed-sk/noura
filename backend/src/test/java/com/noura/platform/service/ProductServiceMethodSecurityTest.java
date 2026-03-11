package com.noura.platform.service;

import com.noura.platform.mapper.ProductMapper;
import com.noura.platform.repository.BrandRepository;
import com.noura.platform.repository.CategoryRepository;
import com.noura.platform.repository.ProductInventoryRepository;
import com.noura.platform.repository.ProductMediaRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.ProductReviewRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.TrendTagRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProductServiceMethodSecurityTest.Config.class)
class ProductServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private ProductService productService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createProduct_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> productService.createProduct(null));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Config {

        @Bean ProductRepository productRepository() { return mock(ProductRepository.class); }
        @Bean CategoryRepository categoryRepository() { return mock(CategoryRepository.class); }
        @Bean BrandRepository brandRepository() { return mock(BrandRepository.class); }
        @Bean ProductVariantRepository productVariantRepository() { return mock(ProductVariantRepository.class); }
        @Bean ProductMediaRepository productMediaRepository() { return mock(ProductMediaRepository.class); }
        @Bean ProductReviewRepository productReviewRepository() { return mock(ProductReviewRepository.class); }
        @Bean ProductInventoryRepository productInventoryRepository() { return mock(ProductInventoryRepository.class); }
        @Bean StoreRepository storeRepository() { return mock(StoreRepository.class); }
        @Bean TrendTagRepository trendTagRepository() { return mock(TrendTagRepository.class); }
        @Bean UserAccountRepository userAccountRepository() { return mock(UserAccountRepository.class); }
        @Bean ProductMapper productMapper() { return mock(ProductMapper.class); }

        @Bean
        ProductService productService(
                ProductRepository productRepository,
                CategoryRepository categoryRepository,
                BrandRepository brandRepository,
                ProductVariantRepository productVariantRepository,
                ProductMediaRepository productMediaRepository,
                ProductReviewRepository productReviewRepository,
                ProductInventoryRepository productInventoryRepository,
                StoreRepository storeRepository,
                TrendTagRepository trendTagRepository,
                UserAccountRepository userAccountRepository,
                ProductMapper productMapper
        ) {
            return new ProductServiceImpl(
                    productRepository,
                    categoryRepository,
                    brandRepository,
                    productVariantRepository,
                    productMediaRepository,
                    productReviewRepository,
                    productInventoryRepository,
                    storeRepository,
                    trendTagRepository,
                    userAccountRepository,
                    productMapper
            );
        }
    }
}
