package com.noura.platform.inventory.service.impl;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
import com.noura.platform.inventory.domain.Category;
import com.noura.platform.inventory.domain.Product;
import com.noura.platform.inventory.dto.category.CategorySummaryResponse;
import com.noura.platform.inventory.dto.product.ProductRequest;
import com.noura.platform.inventory.dto.product.ProductResponse;
import com.noura.platform.inventory.mapper.InventoryProductMapper;
import com.noura.platform.inventory.repository.InventoryCategoryRepository;
import com.noura.platform.inventory.repository.InventoryProductRepository;
import com.noura.platform.inventory.service.ProductService;
import com.noura.platform.service.recovery.RecoveryGovernanceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies governed recovery behavior for inventory products.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private InventoryProductRepository productRepository;

    @Mock
    private InventoryCategoryRepository categoryRepository;

    @Mock
    private InventoryProductMapper productMapper;

    @Mock
    private RecoveryGovernanceService recoveryGovernanceService;

    /**
     * Clears the security context after each test.
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Captures a governed recovery version when a product is created.
     */
    @Test
    void createProductShouldCaptureRecoveryVersion() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("architect@noura.test", "password"));
        ProductService productService = new ProductServiceImpl(productRepository, categoryRepository, productMapper, recoveryGovernanceService);
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
        Category category = new Category();
        category.setId("cat-1");
        category.setCategoryCode("SHOES");
        category.setName("Shoes");
        category.setActive(true);
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
                new CategorySummaryResponse("cat-1", "SHOES", "Shoes", 0, true),
                List.of(new CategorySummaryResponse("cat-1", "SHOES", "Shoes", 0, true)),
                Instant.parse("2026-03-11T00:00:00Z"),
                Instant.parse("2026-03-11T00:00:00Z")
        );

        when(productRepository.existsBySkuIgnoreCaseAndDeletedAtIsNull("SKU-1")).thenReturn(false);
        when(categoryRepository.findByIdAndDeletedAtIsNull("cat-1")).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            if (product.getId() == null) {
                product.setId("prod-1");
            }
            return product;
        });
        when(productMapper.toResponse(any(Product.class))).thenReturn(response);

        ProductResponse created = productService.createProduct(request);

        assertEquals("prod-1", created.id());
        verify(recoveryGovernanceService).captureVersion(
                eq("INVENTORY_PRODUCT"),
                eq("prod-1"),
                eq(RecoveryActionType.CREATE),
                eq("architect@noura.test"),
                eq("Inventory product created."),
                eq(Map.of("source", "inventory-product-service"))
        );
    }

    /**
     * Delegates product deletion to the governed trash action.
     */
    @Test
    void deleteProductShouldDelegateToGovernedTrash() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("architect@noura.test", "password"));
        ProductService productService = new ProductServiceImpl(productRepository, categoryRepository, productMapper, recoveryGovernanceService);
        ArgumentCaptor<RecoveryActionRequest> requestCaptor = ArgumentCaptor.forClass(RecoveryActionRequest.class);

        productService.deleteProduct("prod-1");

        verify(recoveryGovernanceService).applyAction(requestCaptor.capture(), eq("architect@noura.test"));
        assertEquals("INVENTORY_PRODUCT", requestCaptor.getValue().entityType());
        assertEquals("prod-1", requestCaptor.getValue().entityId());
        assertEquals(RecoveryActionType.TRASH, requestCaptor.getValue().actionType());
    }
}
