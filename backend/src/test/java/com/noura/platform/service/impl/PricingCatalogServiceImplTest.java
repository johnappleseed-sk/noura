package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.domain.entity.Price;
import com.noura.platform.domain.entity.PriceList;
import com.noura.platform.domain.entity.ProductVariant;
import com.noura.platform.domain.enums.PriceListType;
import com.noura.platform.dto.pricing.PriceUpsertRequest;
import com.noura.platform.repository.PriceListRepository;
import com.noura.platform.repository.PriceRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.repository.PromotionApplicationRepository;
import com.noura.platform.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingCatalogServiceImplTest {

    @Mock
    private PriceRepository priceRepository;
    @Mock
    private PriceListRepository priceListRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private PromotionApplicationRepository promotionApplicationRepository;

    @Test
    void upsertPrice_shouldUpdateExistingPrice() {
        PricingCatalogServiceImpl service = service();
        UUID variantId = UUID.randomUUID();
        UUID priceListId = UUID.randomUUID();
        UUID priceId = UUID.randomUUID();

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        PriceList priceList = new PriceList();
        priceList.setId(priceListId);
        priceList.setType(PriceListType.BASE);

        Price existing = new Price();
        existing.setId(priceId);
        existing.setVariant(variant);
        existing.setPriceList(priceList);
        existing.setCurrency("USD");
        existing.setAmount(new BigDecimal("50.00"));
        existing.setPriority(1);

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(priceListRepository.findById(priceListId)).thenReturn(Optional.of(priceList));
        when(priceRepository.findByNaturalKey(variantId, priceListId, "USD", null, null)).thenReturn(Optional.of(existing));
        when(priceRepository.save(any(Price.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.upsertPrice(new PriceUpsertRequest(
                variantId,
                priceListId,
                new BigDecimal("39.99"),
                "usd",
                null,
                null,
                5
        ));

        assertEquals(priceId, result.id());
        assertEquals(new BigDecimal("39.99"), result.amount());
        assertEquals("USD", result.currency());
        assertEquals(5, result.priority());
    }

    @Test
    void upsertPrice_shouldRejectInvalidDateWindow() {
        PricingCatalogServiceImpl service = service();
        UUID variantId = UUID.randomUUID();
        UUID priceListId = UUID.randomUUID();
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        PriceList priceList = new PriceList();
        priceList.setId(priceListId);

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(priceListRepository.findById(priceListId)).thenReturn(Optional.of(priceList));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.upsertPrice(new PriceUpsertRequest(
                        variantId,
                        priceListId,
                        new BigDecimal("10.00"),
                        "USD",
                        Instant.parse("2026-03-10T00:00:00Z"),
                        Instant.parse("2026-03-09T00:00:00Z"),
                        1
                ))
        );
        assertEquals("PRICE_WINDOW_INVALID", exception.getCode());
        verify(priceRepository, never()).save(any(Price.class));
    }

    private PricingCatalogServiceImpl service() {
        return new PricingCatalogServiceImpl(
                priceRepository,
                priceListRepository,
                productVariantRepository,
                promotionRepository,
                promotionApplicationRepository
        );
    }
}
