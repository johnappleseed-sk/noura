package com.noura.platform.service.impl;

import com.noura.platform.dto.pricing.PromotionDto;
import com.noura.platform.service.PricingCatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Iterator;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedPricingServiceImplTest {

    @Mock
    private PricingCatalogService pricingCatalogService;

    @Test
    void activePromotionsDelegatesToPricingCatalogService() {
        var expected = List.of(new PromotionDto(
                UUID.randomUUID(),
                "Flash Sale",
                "FLASH-SALE",
                null,
                com.noura.platform.domain.enums.PromotionType.PERCENTAGE,
                null,
                null,
                null,
                null,
                true,
                false,
                10,
                null,
                null,
                0,
                null,
                false,
                null,
                null,
                List.of()
        ));
        when(pricingCatalogService.activePromotions()).thenReturn(expected);

        var service = new UnifiedPricingServiceImpl(pricingCatalogService, skuPricingProvider(null), commercePricingProvider(null));
        List<PromotionDto> actual = service.activePromotions();

        assertThat(actual).isSameAs(expected);
        verify(pricingCatalogService).activePromotions();
    }

    private ObjectProvider<com.noura.platform.commerce.service.SkuUnitPricingService> skuPricingProvider(
            com.noura.platform.commerce.service.SkuUnitPricingService svc) {
        return new ObjectProvider<>() {
            @Override
            public com.noura.platform.commerce.service.SkuUnitPricingService getObject(Object... args) { return getIfAvailable(); }
            @Override
            public com.noura.platform.commerce.service.SkuUnitPricingService getIfAvailable() { return svc; }
            @Override
            public com.noura.platform.commerce.service.SkuUnitPricingService getIfUnique() { return svc; }
            @Override
            public com.noura.platform.commerce.service.SkuUnitPricingService getObject() {
                if (svc == null) throw new IllegalStateException("Commerce SKU pricing service is not active in the current runtime profile.");
                return svc;
            }
            @Override
            public Iterator<com.noura.platform.commerce.service.SkuUnitPricingService> iterator() {
                return svc == null ? List.<com.noura.platform.commerce.service.SkuUnitPricingService>of().iterator() : List.of(svc).iterator();
            }
            @Override
            public Stream<com.noura.platform.commerce.service.SkuUnitPricingService> stream() {
                return svc == null ? Stream.empty() : Stream.of(svc);
            }
        };
    }

    private ObjectProvider<com.noura.platform.commerce.service.PricingService> commercePricingProvider(
            com.noura.platform.commerce.service.PricingService svc) {
        return new ObjectProvider<>() {
            @Override
            public com.noura.platform.commerce.service.PricingService getObject(Object... args) { return getIfAvailable(); }
            @Override
            public com.noura.platform.commerce.service.PricingService getIfAvailable() { return svc; }
            @Override
            public com.noura.platform.commerce.service.PricingService getIfUnique() { return svc; }
            @Override
            public com.noura.platform.commerce.service.PricingService getObject() {
                if (svc == null) throw new IllegalStateException("Commerce pricing service is not active in the current runtime profile.");
                return svc;
            }
            @Override
            public Iterator<com.noura.platform.commerce.service.PricingService> iterator() {
                return svc == null ? List.<com.noura.platform.commerce.service.PricingService>of().iterator() : List.of(svc).iterator();
            }
            @Override
            public Stream<com.noura.platform.commerce.service.PricingService> stream() {
                return svc == null ? Stream.empty() : Stream.of(svc);
            }
        };
    }
}
