package com.noura.pricing.service;

import com.noura.pricing.domain.entity.PricingCurrency;
import com.noura.pricing.domain.entity.ProductPrice;
import com.noura.pricing.dto.price.PriceResolutionResponse;
import com.noura.pricing.dto.price.PriceUpsertRequest;
import com.noura.pricing.dto.price.ProductPriceResponse;
import com.noura.pricing.exception.NotFoundException;
import com.noura.pricing.repository.PricingCurrencyRepository;
import com.noura.pricing.repository.ProductPriceRepository;
import com.noura.pricing.service.impl.ProductPricingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductPricingServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ProductPricingServiceImplTest {

    @Mock
    private ProductPriceRepository productPriceRepository;

    @Mock
    private PricingCurrencyRepository pricingCurrencyRepository;

    private ProductPricingServiceImpl service;

    /**
     * Initializes service under test.
     */
    @BeforeEach
    void setUp() {
        service = new ProductPricingServiceImpl(productPriceRepository, pricingCurrencyRepository);
    }

    /**
     * Verifies store-scoped record wins over channel/global records during resolution.
     */
    @Test
    void resolveProductPricePrefersStoreScope() {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Instant now = Instant.parse("2026-03-16T12:00:00Z");
        PricingCurrency usd = activeCurrency("USD", true);

        ProductPrice global = priceRow(productId, null, null, 10, "120.00", "150.00", true);
        ProductPrice channel = priceRow(productId, null, "WEB", 20, "110.00", "150.00", true);
        ProductPrice store = priceRow(productId, storeId, null, 5, "99.00", "140.00", true);

        when(pricingCurrencyRepository.findById("USD")).thenReturn(Optional.of(usd));
        when(productPriceRepository.findByProductIdAndCurrencyCodeIgnoreCase(productId, "USD"))
                .thenReturn(List.of(global, channel, store));

        PriceResolutionResponse response = service.resolveProductPrice(productId, "USD", storeId, "WEB", now);

        assertThat(response.sourcePriceId()).isEqualTo(store.getId());
        assertThat(response.effectivePrice()).isEqualByComparingTo("99.0000");
        assertThat(response.compareAtPrice()).isEqualByComparingTo("140.0000");
    }

    /**
     * Verifies not-found is thrown when no row is active for the requested timestamp.
     */
    @Test
    void resolveProductPriceThrowsWhenNoActiveRecord() {
        UUID productId = UUID.randomUUID();
        PricingCurrency usd = activeCurrency("USD", true);
        ProductPrice expired = priceRow(
                productId,
                null,
                null,
                1,
                "120.00",
                "150.00",
                true
        );
        expired.setStartsAt(Instant.parse("2025-01-01T00:00:00Z"));
        expired.setEndsAt(Instant.parse("2025-12-31T23:59:59Z"));

        when(pricingCurrencyRepository.findById("USD")).thenReturn(Optional.of(usd));
        when(productPriceRepository.findByProductIdAndCurrencyCodeIgnoreCase(productId, "USD"))
                .thenReturn(List.of(expired));

        assertThatThrownBy(() -> service.resolveProductPrice(
                productId,
                "USD",
                null,
                null,
                Instant.parse("2026-03-16T00:00:00Z")
        )).isInstanceOf(NotFoundException.class);
    }

    /**
     * Verifies upsert creates a new record and persists normalized values.
     */
    @Test
    void upsertPriceCreatesNewRecord() {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        PricingCurrency usd = activeCurrency("USD", true);

        when(pricingCurrencyRepository.findById("USD")).thenReturn(Optional.of(usd));
        when(productPriceRepository.findByNaturalKey(
                eq(productId),
                eq("USD"),
                eq(storeId),
                eq("WEB"),
                any(),
                any()
        )).thenReturn(Optional.empty());
        when(productPriceRepository.save(any(ProductPrice.class)))
                .thenAnswer(invocation -> {
                    ProductPrice saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    saved.setCreatedAt(Instant.parse("2026-03-16T12:00:00Z"));
                    saved.setUpdatedAt(Instant.parse("2026-03-16T12:00:00Z"));
                    return saved;
                });

        ProductPriceResponse response = service.upsertPrice(
                new PriceUpsertRequest(
                        productId,
                        "usd",
                        new BigDecimal("77.5"),
                        new BigDecimal("99"),
                        "web",
                        storeId,
                        Instant.parse("2026-03-16T00:00:00Z"),
                        Instant.parse("2026-12-31T23:59:59Z"),
                        30,
                        true
                ),
                "admin-1"
        );

        assertThat(response.currencyCode()).isEqualTo("USD");
        assertThat(response.channelCode()).isEqualTo("WEB");
        assertThat(response.basePrice()).isEqualByComparingTo("77.5000");
        assertThat(response.compareAtPrice()).isEqualByComparingTo("99.0000");
        assertThat(response.effectivePrice()).isEqualByComparingTo("77.5000");
    }

    /**
     * Creates active currency fixture.
     *
     * @param code currency code
     * @param defaultCurrency default-currency flag
     * @return currency fixture
     */
    private PricingCurrency activeCurrency(String code, boolean defaultCurrency) {
        PricingCurrency currency = new PricingCurrency();
        currency.setCode(code);
        currency.setActive(true);
        currency.setDefaultCurrency(defaultCurrency);
        currency.setDecimalPlaces((short) 2);
        currency.setName(code);
        return currency;
    }

    /**
     * Creates a product price fixture row.
     *
     * @param productId product identifier
     * @param storeId optional store scope
     * @param channelCode optional channel scope
     * @param priority row priority
     * @param basePrice base price value
     * @param compareAt compare-at value
     * @param active active flag
     * @return fixture row
     */
    private ProductPrice priceRow(
            UUID productId,
            UUID storeId,
            String channelCode,
            int priority,
            String basePrice,
            String compareAt,
            boolean active
    ) {
        ProductPrice row = new ProductPrice();
        row.setId(UUID.randomUUID());
        row.setProductId(productId);
        row.setCurrencyCode("USD");
        row.setStoreId(storeId);
        row.setChannelCode(channelCode);
        row.setBasePrice(new BigDecimal(basePrice));
        row.setCompareAtPrice(compareAt == null ? null : new BigDecimal(compareAt));
        row.setPriority(priority);
        row.setActive(active);
        row.setUpdatedAt(Instant.parse("2026-03-16T11:00:00Z"));
        return row;
    }
}
