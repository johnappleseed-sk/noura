package com.noura.platform.service.impl;

import com.noura.platform.commerce.api.v1.dto.product.ApiProductDto;
import com.noura.platform.commerce.api.v1.service.ApiProductService;
import com.noura.platform.dto.catalog.CategoryTreeDto;
import com.noura.platform.service.CatalogManagementService;
import com.noura.platform.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedCatalogServiceImplTest {

    @Mock
    private ProductService platformProductService;

    @Mock
    private CatalogManagementService platformCatalogManagementService;

    @Mock
    private ApiProductService commerceProductService;

    private UnifiedCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UnifiedCatalogServiceImpl(platformProductService, platformCatalogManagementService, provider(null), storefrontCatalogProvider(null));
    }

    @Test
    void listProductsDelegatesToPlatformProductService() {
        var pageable = PageRequest.of(0, 20);
        Page<com.noura.platform.dto.product.ProductDto> expected = Page.empty(pageable);
        when(platformProductService.listProducts(null, pageable)).thenReturn(expected);

        Page<com.noura.platform.dto.product.ProductDto> actual = service.listProducts(null, pageable);

        assertThat(actual).isSameAs(expected);
        verify(platformProductService).listProducts(null, pageable);
    }

    @Test
    void categoryTreeDelegatesToPlatformCatalogService() {
        List<CategoryTreeDto> expected = List.of(new CategoryTreeDto(
                UUID.randomUUID(),
                "Beverages",
                "Beverages",
                "220299",
                UUID.randomUUID(),
                List.of()
        ));
        when(platformCatalogManagementService.categoryTree("en")).thenReturn(expected);

        List<CategoryTreeDto> actual = service.categoryTree("en");

        assertThat(actual).isSameAs(expected);
        verify(platformCatalogManagementService).categoryTree("en");
    }

    @Test
    void listCommerceProductsDelegatesWhenLegacyServiceIsAvailable() {
        var pageable = PageRequest.of(0, 20);
        Page<ApiProductDto> expected = new PageImpl<>(List.of());
        when(commerceProductService.list("tea", 7L, true, false, pageable)).thenReturn(expected);
        service = new UnifiedCatalogServiceImpl(platformProductService, platformCatalogManagementService, provider(commerceProductService), storefrontCatalogProvider(null));

        Page<ApiProductDto> actual = service.listCommerceProducts("tea", 7L, true, false, pageable);

        assertThat(actual).isSameAs(expected);
        verify(commerceProductService).list("tea", 7L, true, false, pageable);
    }

    @Test
    void listCommerceProductsFailsFastWhenLegacyServiceIsInactive() {
        assertThatThrownBy(() -> service.listCommerceProducts("tea", 7L, true, false, PageRequest.of(0, 20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Legacy commerce product service is not active");
    }

    private ObjectProvider<ApiProductService> provider(ApiProductService productService) {
        return new ObjectProvider<>() {
            @Override
            public ApiProductService getObject(Object... args) {
                return getIfAvailable();
            }

            @Override
            public ApiProductService getIfAvailable() {
                return productService;
            }

            @Override
            public ApiProductService getIfUnique() {
                return productService;
            }

            @Override
            public ApiProductService getObject() {
                if (productService == null) {
                    throw new IllegalStateException("Legacy commerce product service is not active in the current runtime profile.");
                }
                return productService;
            }

            @Override
            public Iterator<ApiProductService> iterator() {
                return productService == null ? List.<ApiProductService>of().iterator() : List.of(productService).iterator();
            }

            @Override
            public Stream<ApiProductService> stream() {
                return productService == null ? Stream.empty() : Stream.of(productService);
            }
        };
    }

    private ObjectProvider<com.noura.platform.commerce.catalog.application.StorefrontCatalogService> storefrontCatalogProvider(
            com.noura.platform.commerce.catalog.application.StorefrontCatalogService svc) {
        return new ObjectProvider<>() {
            @Override
            public com.noura.platform.commerce.catalog.application.StorefrontCatalogService getObject(Object... args) { return getIfAvailable(); }
            @Override
            public com.noura.platform.commerce.catalog.application.StorefrontCatalogService getIfAvailable() { return svc; }
            @Override
            public com.noura.platform.commerce.catalog.application.StorefrontCatalogService getIfUnique() { return svc; }
            @Override
            public com.noura.platform.commerce.catalog.application.StorefrontCatalogService getObject() {
                if (svc == null) throw new IllegalStateException("Storefront catalog service is not active in the current runtime profile.");
                return svc;
            }
            @Override
            public Iterator<com.noura.platform.commerce.catalog.application.StorefrontCatalogService> iterator() {
                return svc == null ? List.<com.noura.platform.commerce.catalog.application.StorefrontCatalogService>of().iterator() : List.of(svc).iterator();
            }
            @Override
            public Stream<com.noura.platform.commerce.catalog.application.StorefrontCatalogService> stream() {
                return svc == null ? Stream.empty() : Stream.of(svc);
            }
        };
    }
}
