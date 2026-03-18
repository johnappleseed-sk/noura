package com.noura.catalog.service.impl;

import com.noura.catalog.domain.enums.ProductStatus;
import com.noura.catalog.dto.admin.MerchandisingBoostRequest;
import com.noura.catalog.dto.admin.RecommendationSettingsUpdateRequest;
import com.noura.catalog.dto.product.ProductDto;
import com.noura.catalog.dto.product.ProductSeoDto;
import com.noura.catalog.service.CatalogQueryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CatalogAdminCompatibilityServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class CatalogAdminCompatibilityServiceImplTest {

    @Mock
    private CatalogQueryService catalogQueryService;

    /**
     * Verifies recommendation settings can be updated and reflected in preview output.
     */
    @Test
    void shouldUpdateRecommendationSettings() {
        CatalogAdminCompatibilityServiceImpl service = new CatalogAdminCompatibilityServiceImpl(catalogQueryService);

        var updated = service.updateRecommendationSettings(
                new RecommendationSettingsUpdateRequest(2.0, 5.0, 9.0, 31.0, 21.0, 6.0, 7.0, 4.0, 6.0, 61.0, 10),
                "admin"
        );

        Assertions.assertEquals(2.0, updated.productViewWeight());
        Assertions.assertEquals(10, updated.maxRecommendations());
    }

    /**
     * Verifies merchandising preview and boost responses preserve the current admin-web shape.
     */
    @Test
    void shouldPreviewMerchandisingWithManualBoost() {
        CatalogAdminCompatibilityServiceImpl service = new CatalogAdminCompatibilityServiceImpl(catalogQueryService);
        ProductDto product = sampleProduct(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Atlas Backpack",
                true,
                true,
                4.7,
                24,
                90
        );

        when(catalogQueryService.getProduct(eq(product.id()))).thenReturn(product);
        when(catalogQueryService.listProducts(
                eq("atlas"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(product)));
        when(catalogQueryService.trendingRecommendations(anyInt())).thenReturn(List.of(product));
        when(catalogQueryService.bestSellerRecommendations(anyInt())).thenReturn(List.of(product));

        service.createMerchandisingBoost(
                new MerchandisingBoostRequest(product.id(), "Homepage hero", 12.0, true, null, null),
                "admin"
        );

        var preview = service.previewMerchandising("atlas", null, null, 8);

        Assertions.assertEquals(1, preview.featured().size());
        Assertions.assertEquals("Atlas Backpack", preview.featured().getFirst().name());
        Assertions.assertEquals(1, preview.activeBoosts().size());
        Assertions.assertEquals("Homepage hero", preview.activeBoosts().getFirst().label());
    }

    private ProductDto sampleProduct(
            UUID productId,
            String name,
            boolean trending,
            boolean bestSeller,
            double rating,
            int reviewCount,
            int popularityScore
    ) {
        return new ProductDto(
                productId,
                name,
                "Bags",
                "Noura",
                new BigDecimal("42.0000"),
                false,
                trending,
                bestSeller,
                rating,
                reviewCount,
                popularityScore,
                "Short",
                "Long",
                "SEO title",
                "SEO description",
                "atlas-backpack",
                new ProductSeoDto("atlas-backpack", "SEO title", "SEO description"),
                Map.of("categoryId", UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")),
                ProductStatus.PUBLISHED,
                true,
                false,
                List.of(),
                List.of(),
                List.of(),
                "Long",
                "Unisex",
                null,
                null
        );
    }
}
