package com.noura.platform.service.impl;

import com.noura.platform.common.api.PageResponse;
import com.noura.platform.domain.entity.AnalyticsEventRecord;
import com.noura.platform.domain.entity.MerchandisingSettings;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductInventory;
import com.noura.platform.domain.enums.AnalyticsEventType;
import com.noura.platform.dto.merchandising.MerchandisingProductDto;
import com.noura.platform.repository.AnalyticsEventRecordRepository;
import com.noura.platform.repository.MerchandisingBoostRepository;
import com.noura.platform.repository.MerchandisingSettingsRepository;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.ProductInventoryRepository;
import com.noura.platform.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchandisingServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductInventoryRepository productInventoryRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private MerchandisingSettingsRepository merchandisingSettingsRepository;

    @Mock
    private MerchandisingBoostRepository merchandisingBoostRepository;

    @Mock
    private AnalyticsEventRecordRepository analyticsEventRecordRepository;

    @InjectMocks
    private MerchandisingServiceImpl merchandisingService;

    @Test
    void featuredSortUsesClickAndImpressionSignalsInsteadOfSalesProxy() {
        Product highCtr = product("High CTR", 10, false, false, Instant.now());
        Product lowCtr = product("Low CTR", 10, false, false, Instant.now());

        when(merchandisingSettingsRepository.findAll()).thenReturn(List.of(defaultSettings()));
        when(productRepository.findAll()).thenReturn(List.of(lowCtr, highCtr));
        when(productInventoryRepository.findAll()).thenReturn(List.of(inventory(highCtr, 12), inventory(lowCtr, 12)));
        when(orderItemRepository.findAll()).thenReturn(List.of());
        when(merchandisingBoostRepository.findAll()).thenReturn(List.of());
        when(analyticsEventRecordRepository.findAll()).thenReturn(analyticsSignals(highCtr.getId(), 20, 8, lowCtr.getId(), 20, 1));

        PageResponse<MerchandisingProductDto> page = merchandisingService.listProducts(null, null, null, "featured", 0, 12);

        assertEquals(highCtr.getId(), page.getContent().get(0).id());
        assertEquals(lowCtr.getId(), page.getContent().get(1).id());
    }

    @Test
    void priceAscendingUsesEffectiveSalePrice() {
        Product sale = product("Sale", 1, false, false, Instant.now());
        sale.getAttributes().put("salePrice", "9.99");
        sale.setBasePrice(BigDecimal.valueOf(19.99));

        Product full = product("Full", 1, false, false, Instant.now());
        full.setBasePrice(BigDecimal.valueOf(14.99));

        when(merchandisingSettingsRepository.findAll()).thenReturn(List.of(defaultSettings()));
        when(productRepository.findAll()).thenReturn(List.of(full, sale));
        when(productInventoryRepository.findAll()).thenReturn(List.of());
        when(orderItemRepository.findAll()).thenReturn(List.of());
        when(merchandisingBoostRepository.findAll()).thenReturn(List.of());
        when(analyticsEventRecordRepository.findAll()).thenReturn(List.of());

        PageResponse<MerchandisingProductDto> page = merchandisingService.listProducts(null, null, null, "priceAsc", 0, 12);

        assertEquals(sale.getId(), page.getContent().get(0).id());
        assertEquals(BigDecimal.valueOf(19.99), page.getContent().get(0).compareAtPrice());
    }

    private MerchandisingSettings defaultSettings() {
        MerchandisingSettings settings = new MerchandisingSettings();
        settings.setPopularityWeight(1D);
        settings.setInventoryWeight(0.5D);
        settings.setImpressionWeight(0.75D);
        settings.setClickWeight(4D);
        settings.setClickThroughRateWeight(0.6D);
        settings.setManualBoostWeight(1D);
        settings.setNewArrivalWindowDays(30);
        settings.setNewArrivalBoost(25D);
        settings.setTrendingBoost(20D);
        settings.setBestSellerBoost(15D);
        settings.setLowStockPenalty(20D);
        settings.setMaxPageSize(48);
        return settings;
    }

    private Product product(String name, int popularityScore, boolean trending, boolean bestSeller, Instant createdAt) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setBasePrice(BigDecimal.valueOf(12.50));
        product.setAttributes(new LinkedHashMap<>());
        product.setPopularityScore(popularityScore);
        product.setTrending(trending);
        product.setBestSeller(bestSeller);
        product.setActive(true);
        product.setCreatedAt(createdAt);
        return product;
    }

    private ProductInventory inventory(Product product, int stock) {
        ProductInventory inventory = new ProductInventory();
        inventory.setProduct(product);
        inventory.setStore(null);
        inventory.setStock(stock);
        inventory.setStorePrice(product.getBasePrice());
        return inventory;
    }

    private List<AnalyticsEventRecord> analyticsSignals(UUID primaryProductId, int primaryImpressions, int primaryClicks, UUID secondaryProductId, int secondaryImpressions, int secondaryClicks) {
        List<AnalyticsEventRecord> events = new ArrayList<>();
        appendSignals(events, primaryProductId, primaryImpressions, primaryClicks);
        appendSignals(events, secondaryProductId, secondaryImpressions, secondaryClicks);
        return events;
    }

    private void appendSignals(List<AnalyticsEventRecord> events, UUID productId, int impressions, int clicks) {
        for (int index = 0; index < impressions; index++) {
            AnalyticsEventRecord event = new AnalyticsEventRecord();
            event.setEventType(AnalyticsEventType.PRODUCT_IMPRESSION);
            event.setProductId(productId.toString());
            event.setOccurredAt(Instant.now());
            events.add(event);
        }
        for (int index = 0; index < clicks; index++) {
            AnalyticsEventRecord event = new AnalyticsEventRecord();
            event.setEventType(AnalyticsEventType.PRODUCT_CLICK);
            event.setProductId(productId.toString());
            event.setOccurredAt(Instant.now());
            events.add(event);
        }
    }
}
