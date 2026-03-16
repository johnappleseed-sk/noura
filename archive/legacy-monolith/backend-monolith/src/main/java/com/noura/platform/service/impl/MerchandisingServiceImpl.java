package com.noura.platform.service.impl;

import com.noura.platform.common.api.PageResponse;
import com.noura.platform.domain.entity.AnalyticsEventRecord;
import com.noura.platform.domain.entity.MerchandisingBoost;
import com.noura.platform.domain.entity.MerchandisingSettings;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductInventory;
import com.noura.platform.domain.enums.AnalyticsEventType;
import com.noura.platform.dto.merchandising.MerchandisingBoostDto;
import com.noura.platform.dto.merchandising.MerchandisingPreviewDto;
import com.noura.platform.dto.merchandising.MerchandisingProductDto;
import com.noura.platform.dto.merchandising.MerchandisingSettingsDto;
import com.noura.platform.repository.AnalyticsEventRecordRepository;
import com.noura.platform.repository.MerchandisingBoostRepository;
import com.noura.platform.repository.MerchandisingSettingsRepository;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.ProductInventoryRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.service.MerchandisingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchandisingServiceImpl implements MerchandisingService {

    private final ProductRepository productRepository;
    private final ProductInventoryRepository productInventoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final MerchandisingSettingsRepository merchandisingSettingsRepository;
    private final MerchandisingBoostRepository merchandisingBoostRepository;
    private final AnalyticsEventRecordRepository analyticsEventRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MerchandisingProductDto> listProducts(String query, UUID categoryId, UUID storeId, String sort, int page, int size) {
        MerchandisingSettings settings = currentSettings();
        int safePage = Math.max(page, 0);
        int safeSize = sanitizeSize(size, settings);
        List<MerchandisingProductDto> rankedProducts = rankedProducts(query, categoryId, storeId, sort, safeSize * (safePage + 1), settings);
        int fromIndex = Math.min(safePage * safeSize, rankedProducts.size());
        int toIndex = Math.min(fromIndex + safeSize, rankedProducts.size());
        PageImpl<MerchandisingProductDto> pageResult = new PageImpl<>(
                rankedProducts.subList(fromIndex, toIndex),
                PageRequest.of(safePage, safeSize),
                rankedProducts.size()
        );
        return PageResponse.from(pageResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchandisingProductDto> listProductsForPreview(String query, UUID categoryId, UUID storeId, String sort, int limit) {
        MerchandisingSettings settings = currentSettings();
        return rankedProducts(query, categoryId, storeId, sort, sanitizeSize(limit, settings), settings);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchandisingPreviewDto preview(String query, UUID categoryId, UUID storeId, int limit) {
        MerchandisingSettings settings = currentSettings();
        int effectiveLimit = sanitizeSize(limit, settings);
        Instant now = Instant.now();
        return new MerchandisingPreviewDto(
                toSettingsDto(settings),
                categoryId,
                storeId,
                query,
                rankedProducts(query, categoryId, storeId, "featured", effectiveLimit, settings),
                rankedProducts(query, categoryId, storeId, "popularity", effectiveLimit, settings),
                rankedProducts(query, categoryId, storeId, "trending", effectiveLimit, settings),
                rankedProducts(query, categoryId, storeId, "bestselling", effectiveLimit, settings),
                rankedProducts(query, categoryId, storeId, "new", effectiveLimit, settings),
                activeBoostDtos(now)
        );
    }

    private List<MerchandisingProductDto> rankedProducts(
            String query,
            UUID categoryId,
            UUID storeId,
            String sort,
            int limit,
            MerchandisingSettings settings
    ) {
        Instant now = Instant.now();
        String normalizedQuery = normalizeQuery(query);
        Map<UUID, Integer> stockByProduct = aggregateStock(storeId);
        Map<UUID, Integer> salesByProduct = aggregateSales();
        Map<UUID, BehaviorSignals> behaviorByProduct = aggregateBehaviorSignals();
        Map<UUID, Double> boostByProduct = aggregateManualBoosts(now);

        Comparator<Product> comparator = comparatorFor(sort, settings, stockByProduct, salesByProduct, behaviorByProduct, boostByProduct, now);

        return productRepository.findAll().stream()
                .filter(this::isVisible)
                .filter(product -> matchesQuery(product, normalizedQuery))
                .filter(product -> categoryId == null || (product.getCategory() != null && Objects.equals(product.getCategory().getId(), categoryId)))
                .sorted(comparator)
                .limit(limit)
                .map(product -> toDto(product, settings, stockByProduct, behaviorByProduct, boostByProduct, now))
                .toList();
    }

    private Comparator<Product> comparatorFor(
            String sort,
            MerchandisingSettings settings,
            Map<UUID, Integer> stockByProduct,
            Map<UUID, Integer> salesByProduct,
            Map<UUID, BehaviorSignals> behaviorByProduct,
            Map<UUID, Double> boostByProduct,
            Instant now
    ) {
        String normalizedSort = normalizeSort(sort);
        Comparator<Product> fallback = Comparator.comparing(
                (Product product) -> merchandisingScore(product, settings, stockByProduct, behaviorByProduct, boostByProduct, now)
        ).reversed().thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER);

        return switch (normalizedSort) {
            case "name" -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case "priceasc" -> Comparator.comparing(this::effectivePrice).thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case "pricedesc" -> Comparator.comparing(this::effectivePrice).reversed().thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case "popularity" -> Comparator.comparingInt(Product::getPopularityScore).reversed().thenComparing(fallback);
            case "trending" -> Comparator.comparing(Product::isTrending).reversed().thenComparing(fallback);
            case "bestselling" -> Comparator.comparing((Product product) -> salesByProduct.getOrDefault(product.getId(), 0)).reversed().thenComparing(fallback);
            case "new" -> Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(fallback);
            default -> fallback;
        };
    }

    private MerchandisingProductDto toDto(
            Product product,
            MerchandisingSettings settings,
            Map<UUID, Integer> stockByProduct,
            Map<UUID, BehaviorSignals> behaviorByProduct,
            Map<UUID, Double> boostByProduct,
            Instant now
    ) {
        int stockQty = stockByProduct.getOrDefault(product.getId(), 0);
        BigDecimal effectivePrice = effectivePrice(product);
        BigDecimal compareAtPrice = compareAtPrice(product, effectivePrice);
        boolean isNew = isNewArrival(product, settings, now);
        return new MerchandisingProductDto(
                product.getId(),
                product.getName(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                effectivePrice,
                compareAtPrice,
                imageUrl(product),
                stockQty,
                stockQty > 0 && stockQty <= 5,
                product.isAllowBackorder(),
                isNew,
                product.isTrending(),
                product.isBestSeller(),
                merchandisingScore(product, settings, stockByProduct, behaviorByProduct, boostByProduct, now)
        );
    }

    private double merchandisingScore(
            Product product,
            MerchandisingSettings settings,
            Map<UUID, Integer> stockByProduct,
            Map<UUID, BehaviorSignals> behaviorByProduct,
            Map<UUID, Double> boostByProduct,
            Instant now
    ) {
        int stockQty = stockByProduct.getOrDefault(product.getId(), 0);
        BehaviorSignals behavior = behaviorByProduct.getOrDefault(product.getId(), BehaviorSignals.empty());
        double manualBoost = boostByProduct.getOrDefault(product.getId(), 0D);
        double score = product.getPopularityScore() * settings.getPopularityWeight();
        score += Math.min(stockQty, 50) * settings.getInventoryWeight();
        score += Math.log1p(behavior.impressions()) * settings.getImpressionWeight();
        score += Math.log1p(behavior.clicks()) * settings.getClickWeight();
        score += behavior.clickThroughRatePercent() * settings.getClickThroughRateWeight();
        score += manualBoost * settings.getManualBoostWeight();

        // Featured ranking now uses direct click/impression behavior instead of sales velocity as a conversion proxy.
        if (isNewArrival(product, settings, now)) {
            score += settings.getNewArrivalBoost();
        }
        if (product.isTrending()) {
            score += settings.getTrendingBoost();
        }
        if (product.isBestSeller()) {
            score += settings.getBestSellerBoost();
        }
        if (stockQty <= 0 && !product.isAllowBackorder()) {
            score -= settings.getLowStockPenalty();
        } else if (stockQty > 0 && stockQty <= 5) {
            score -= settings.getLowStockPenalty() / 2D;
        }
        return score;
    }

    private Map<UUID, Integer> aggregateStock(UUID storeId) {
        Map<UUID, Integer> stockByProduct = new LinkedHashMap<>();
        for (ProductInventory inventory : productInventoryRepository.findAll()) {
            if (storeId != null && (inventory.getStore() == null || !Objects.equals(inventory.getStore().getId(), storeId))) {
                continue;
            }
            stockByProduct.merge(inventory.getProduct().getId(), inventory.getStock(), Integer::sum);
        }
        return stockByProduct;
    }

    private Map<UUID, Integer> aggregateSales() {
        Map<UUID, Integer> salesByProduct = new LinkedHashMap<>();
        orderItemRepository.findAll().forEach(item -> salesByProduct.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum));
        return salesByProduct;
    }

    private Map<UUID, BehaviorSignals> aggregateBehaviorSignals() {
        Map<UUID, Long> impressions = new LinkedHashMap<>();
        Map<UUID, Long> clicks = new LinkedHashMap<>();
        for (AnalyticsEventRecord event : analyticsEventRecordRepository.findAll()) {
            UUID productId = parseUuid(event.getProductId());
            if (productId == null) {
                continue;
            }
            if (event.getEventType() == AnalyticsEventType.PRODUCT_IMPRESSION) {
                impressions.merge(productId, 1L, Long::sum);
            } else if (event.getEventType() == AnalyticsEventType.PRODUCT_CLICK) {
                clicks.merge(productId, 1L, Long::sum);
            }
        }
        Map<UUID, BehaviorSignals> behaviorByProduct = new LinkedHashMap<>();
        for (Map.Entry<UUID, Long> entry : impressions.entrySet()) {
            behaviorByProduct.put(entry.getKey(), new BehaviorSignals(entry.getValue(), clicks.getOrDefault(entry.getKey(), 0L)));
        }
        for (Map.Entry<UUID, Long> entry : clicks.entrySet()) {
            behaviorByProduct.putIfAbsent(entry.getKey(), new BehaviorSignals(impressions.getOrDefault(entry.getKey(), 0L), entry.getValue()));
        }
        return behaviorByProduct;
    }

    private Map<UUID, Double> aggregateManualBoosts(Instant now) {
        Map<UUID, Double> boosts = new LinkedHashMap<>();
        for (MerchandisingBoost boost : activeBoosts(now)) {
            boosts.merge(boost.getProduct().getId(), boost.getBoostValue(), Double::sum);
        }
        return boosts;
    }

    private List<MerchandisingBoostDto> activeBoostDtos(Instant now) {
        return activeBoosts(now).stream()
                .map(this::toBoostDto)
                .toList();
    }

    private List<MerchandisingBoost> activeBoosts(Instant now) {
        return merchandisingBoostRepository.findAll().stream()
                .filter(MerchandisingBoost::isActive)
                // Scheduled boosts only participate while their configured time window is open.
                .filter(boost -> boost.getStartAt() == null || !boost.getStartAt().isAfter(now))
                .filter(boost -> boost.getEndAt() == null || !boost.getEndAt().isBefore(now))
                .sorted(Comparator.comparing(MerchandisingBoost::getLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private MerchandisingBoostDto toBoostDto(MerchandisingBoost boost) {
        return new MerchandisingBoostDto(
                boost.getId(),
                boost.getProduct().getId(),
                boost.getProduct().getName(),
                boost.getLabel(),
                boost.getBoostValue(),
                boost.isActive(),
                boost.getStartAt(),
                boost.getEndAt()
        );
    }

    private MerchandisingSettings currentSettings() {
        return merchandisingSettingsRepository.findAll().stream().findFirst().orElseGet(MerchandisingSettings::new);
    }

    private MerchandisingSettingsDto toSettingsDto(MerchandisingSettings settings) {
        return new MerchandisingSettingsDto(
                settings.getId(),
                settings.getPopularityWeight(),
                settings.getInventoryWeight(),
                settings.getImpressionWeight(),
                settings.getClickWeight(),
                settings.getClickThroughRateWeight(),
                settings.getManualBoostWeight(),
                settings.getNewArrivalWindowDays(),
                settings.getNewArrivalBoost(),
                settings.getTrendingBoost(),
                settings.getBestSellerBoost(),
                settings.getLowStockPenalty(),
                settings.getMaxPageSize()
        );
    }

    private boolean matchesQuery(Product product, String query) {
        if (query == null) {
            return true;
        }
        String haystack = (product.getName() + " " + (product.getShortDescription() == null ? "" : product.getShortDescription())).toLowerCase(Locale.ROOT);
        return haystack.contains(query);
    }

    private boolean isVisible(Product product) {
        return product != null && product.isActive() && product.getDeletedAt() == null;
    }

    private boolean isNewArrival(Product product, MerchandisingSettings settings, Instant now) {
        if (product.getCreatedAt() == null) {
            return false;
        }
        return product.getCreatedAt().isAfter(now.minus(settings.getNewArrivalWindowDays(), ChronoUnit.DAYS));
    }

    private BigDecimal effectivePrice(Product product) {
        BigDecimal salePrice = decimalAttribute(product, "salePrice");
        if (salePrice == null) {
            salePrice = decimalAttribute(product, "sale_price");
        }
        if (salePrice != null && product.getBasePrice() != null && salePrice.compareTo(product.getBasePrice()) < 0) {
            return salePrice;
        }
        return product.getBasePrice();
    }

    private BigDecimal compareAtPrice(Product product, BigDecimal effectivePrice) {
        if (product.getBasePrice() == null || effectivePrice == null) {
            return null;
        }
        return product.getBasePrice().compareTo(effectivePrice) > 0 ? product.getBasePrice() : null;
    }

    private BigDecimal decimalAttribute(Product product, String key) {
        if (product.getAttributes() == null) {
            return null;
        }
        Object raw = product.getAttributes().get(key);
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (raw instanceof String string && !string.isBlank()) {
            try {
                return new BigDecimal(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String imageUrl(Product product) {
        if (product.getAttributes() == null) {
            return null;
        }
        Object value = product.getAttributes().get("imageUrl");
        if (value == null) {
            value = product.getAttributes().get("image_url");
        }
        return value == null ? null : value.toString();
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? null : query.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "featured";
        }
        return sort.replace("-", "").replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    private int sanitizeSize(int size, MerchandisingSettings settings) {
        int max = settings.getMaxPageSize() <= 0 ? 48 : settings.getMaxPageSize();
        return Math.max(1, Math.min(size, max));
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private record BehaviorSignals(long impressions, long clicks) {
        private static BehaviorSignals empty() {
            return new BehaviorSignals(0L, 0L);
        }

        private double clickThroughRatePercent() {
            if (impressions <= 0L) {
                return 0D;
            }
            return (clicks * 100D) / impressions;
        }
    }
}
