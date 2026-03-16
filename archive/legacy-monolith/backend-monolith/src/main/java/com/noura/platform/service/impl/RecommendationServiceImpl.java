package com.noura.platform.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.AnalyticsEventRecord;
import com.noura.platform.domain.entity.OrderItem;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.RecommendationSettings;
import com.noura.platform.domain.enums.AnalyticsEventType;
import com.noura.platform.dto.recommendation.ProductRecommendationResponse;
import com.noura.platform.dto.recommendation.RecommendationProductDto;
import com.noura.platform.repository.AnalyticsEventRecordRepository;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.RecommendationSettingsRepository;
import com.noura.platform.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final AnalyticsEventRecordRepository analyticsEventRecordRepository;
    private final RecommendationSettingsRepository recommendationSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductRecommendationResponse getProductRecommendations(UUID productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found."));
        RecommendationSettings settings = currentSettings();
        int effectiveLimit = sanitizeLimit(limit, settings);
        return new ProductRecommendationResponse(
                productId,
                relatedProducts(product, effectiveLimit, settings),
                frequentlyBoughtTogether(productId, effectiveLimit, settings),
                trendingRecommendationsInternal(effectiveLimit, settings)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationProductDto> getTrendingRecommendations(int limit) {
        RecommendationSettings settings = currentSettings();
        return trendingRecommendationsInternal(sanitizeLimit(limit, settings), settings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationProductDto> getBestSellerRecommendations(int limit) {
        RecommendationSettings settings = currentSettings();
        int effectiveLimit = sanitizeLimit(limit, settings);
        Map<UUID, Double> scores = new LinkedHashMap<>();
        for (Product product : visibleProducts()) {
            double score = product.getPopularityScore();
            if (product.isBestSeller()) {
                score += settings.getBestSellerBoost() * 3;
            }
            if (product.isTrending()) {
                score += settings.getTrendingBoost() * 0.35;
            }
            score += product.getAverageRating() * settings.getRatingWeight();
            scores.put(product.getId(), score);
        }
        return topProducts(scores, effectiveLimit, "Best seller");
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationProductDto> getDealRecommendations(int limit) {
        RecommendationSettings settings = currentSettings();
        int effectiveLimit = sanitizeLimit(limit, settings);
        Map<UUID, Double> scores = new LinkedHashMap<>();
        for (Product product : visibleProducts()) {
            BigDecimal salePrice = extractSalePrice(product);
            double score = 0D;
            if (product.isFlashSale()) {
                score += settings.getDealBoost();
            }
            if (salePrice != null && product.getBasePrice() != null && product.getBasePrice().signum() > 0 && salePrice.compareTo(product.getBasePrice()) < 0) {
                double discountRatio = product.getBasePrice()
                        .subtract(salePrice)
                        .divide(product.getBasePrice(), 4, RoundingMode.HALF_UP)
                        .doubleValue();
                score += discountRatio * 100;
            }
            if (score <= 0) {
                continue;
            }
            score += product.getPopularityScore() * 0.25;
            scores.put(product.getId(), score);
        }
        return topProducts(scores, effectiveLimit, "Limited-time deal");
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationProductDto> getPersonalizedRecommendations(String customerRef, int limit) {
        RecommendationSettings settings = currentSettings();
        int effectiveLimit = sanitizeLimit(limit, settings);
        String normalizedCustomerRef = normalizeCustomerRef(customerRef);
        if (normalizedCustomerRef == null) {
            return trendingRecommendationsInternal(effectiveLimit, settings);
        }

        List<AnalyticsEventRecord> customerEvents = customerEvents(normalizedCustomerRef);
        Map<UUID, Double> anchorWeights = buildAnchorWeights(customerEvents, false, settings);
        if (anchorWeights.isEmpty()) {
            return trendingRecommendationsInternal(effectiveLimit, settings);
        }

        Set<UUID> anchorIds = anchorWeights.keySet();
        Map<UUID, Product> anchors = productRepository.findAllById(anchorIds).stream()
                .filter(this::isVisible)
                .collect(Collectors.toMap(Product::getId, item -> item));

        Map<UUID, Double> scores = new LinkedHashMap<>();
        for (Product candidate : visibleProducts()) {
            if (anchorIds.contains(candidate.getId())) {
                continue;
            }
            double score = candidate.getPopularityScore() * 0.25 + candidate.getAverageRating() * settings.getRatingWeight() * 0.4;
            if (candidate.isTrending()) {
                score += settings.getTrendingBoost() * 0.25;
            }
            for (Map.Entry<UUID, Double> anchorEntry : anchorWeights.entrySet()) {
                Product anchor = anchors.get(anchorEntry.getKey());
                if (anchor == null) {
                    continue;
                }
                if (sameCategory(anchor, candidate)) {
                    score += anchorEntry.getValue() * settings.getCategoryAffinityWeight();
                }
                if (sameBrand(anchor, candidate)) {
                    score += anchorEntry.getValue() * settings.getBrandAffinityWeight();
                }
            }
            if (score > 0) {
                scores.put(candidate.getId(), score);
            }
        }

        // Co-purchase boosts keep recommendations grounded in proven basket behavior, not only clickstream affinity.
        boostCoPurchasedProducts(scores, anchorIds, settings.getCoPurchaseWeight());
        if (scores.isEmpty()) {
            return trendingRecommendationsInternal(effectiveLimit, settings);
        }
        return topProducts(scores, effectiveLimit, "Personalized for you");
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationProductDto> getCrossSellRecommendations(String customerRef, int limit) {
        RecommendationSettings settings = currentSettings();
        int effectiveLimit = sanitizeLimit(limit, settings);
        String normalizedCustomerRef = normalizeCustomerRef(customerRef);
        if (normalizedCustomerRef == null) {
            return getBestSellerRecommendations(effectiveLimit);
        }

        Map<UUID, Double> anchorWeights = buildAnchorWeights(customerEvents(normalizedCustomerRef), true, settings);
        if (anchorWeights.isEmpty()) {
            return getPersonalizedRecommendations(normalizedCustomerRef, effectiveLimit);
        }

        Map<UUID, Double> scores = new LinkedHashMap<>();
        boostCoPurchasedProducts(scores, anchorWeights.keySet(), settings.getCoPurchaseWeight() * 1.5);
        if (scores.isEmpty()) {
            return getPersonalizedRecommendations(normalizedCustomerRef, effectiveLimit);
        }
        return topProducts(scores, effectiveLimit, "Pairs well with your activity");
    }

    private List<RecommendationProductDto> trendingRecommendationsInternal(int limit, RecommendationSettings settings) {
        Map<UUID, Double> scores = new LinkedHashMap<>();
        for (Product product : visibleProducts()) {
            double score = product.getPopularityScore();
            if (product.isTrending()) {
                score += settings.getTrendingBoost();
            }
            if (product.isBestSeller()) {
                score += settings.getBestSellerBoost();
            }
            score += product.getAverageRating() * settings.getRatingWeight();
            scores.put(product.getId(), score);
        }
        for (AnalyticsEventRecord event : analyticsEventRecordRepository.findAll()) {
            UUID productId = parseUuid(event.getProductId());
            if (productId == null || !scores.containsKey(productId)) {
                continue;
            }
            scores.put(productId, scores.get(productId) + signalWeight(event.getEventType(), false, settings));
        }
        return topProducts(scores, limit, "Trending now");
    }

    private List<RecommendationProductDto> relatedProducts(Product baseProduct, int limit, RecommendationSettings settings) {
        Map<UUID, Double> scores = new LinkedHashMap<>();
        for (Product candidate : visibleProducts()) {
            if (candidate.getId().equals(baseProduct.getId())) {
                continue;
            }
            double score = 0D;
            if (sameCategory(baseProduct, candidate)) {
                score += settings.getCategoryAffinityWeight() * 4;
            }
            if (sameBrand(baseProduct, candidate)) {
                score += settings.getBrandAffinityWeight() * 4;
            }
            score += candidate.getPopularityScore();
            if (score > 0) {
                scores.put(candidate.getId(), score);
            }
        }
        for (AnalyticsEventRecord event : analyticsEventRecordRepository.findAll()) {
            UUID candidateId = parseUuid(event.getProductId());
            if (candidateId == null || candidateId.equals(baseProduct.getId()) || !scores.containsKey(candidateId)) {
                continue;
            }
            if (event.getEventType() == AnalyticsEventType.ADD_TO_CART || event.getEventType() == AnalyticsEventType.PRODUCT_VIEW) {
                scores.put(candidateId, scores.get(candidateId) + settings.getProductViewWeight() * 1.5);
            }
        }
        return topProducts(scores, limit, "Related recommendation");
    }

    private List<RecommendationProductDto> frequentlyBoughtTogether(UUID productId, int limit, RecommendationSettings settings) {
        Map<UUID, List<OrderItem>> orderItemsByOrder = orderItemRepository.findAll().stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
        Map<UUID, Double> scores = new LinkedHashMap<>();
        for (List<OrderItem> items : orderItemsByOrder.values()) {
            Set<UUID> orderedProductIds = items.stream()
                    .map(item -> item.getProduct().getId())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!orderedProductIds.contains(productId)) {
                continue;
            }
            for (OrderItem item : items) {
                UUID candidateId = item.getProduct().getId();
                if (candidateId.equals(productId) || !isVisible(item.getProduct())) {
                    continue;
                }
                scores.merge(candidateId, item.getQuantity() * settings.getCoPurchaseWeight(), Double::sum);
            }
        }
        return topProducts(scores, limit, "Frequently bought together");
    }

    private List<RecommendationProductDto> topProducts(Map<UUID, Double> scores, int limit, String reason) {
        List<UUID> orderedIds = scores.entrySet().stream()
                .sorted(
                        Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder())
                                .thenComparing(entry -> entry.getKey().toString())
                )
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
        if (orderedIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, Product> products = productRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(Product::getId, item -> item));
        List<RecommendationProductDto> response = new ArrayList<>();
        for (UUID productId : orderedIds) {
            Product product = products.get(productId);
            if (product == null) {
                continue;
            }
            response.add(toDto(product, scores.getOrDefault(productId, 0D), reason));
        }
        return response;
    }

    private RecommendationProductDto toDto(Product product, double score, String reason) {
        return new RecommendationProductDto(
                product.getId(),
                product.getName(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getBasePrice(),
                imageUrl(product),
                product.getShortDescription(),
                score,
                reason
        );
    }

    private List<Product> visibleProducts() {
        return productRepository.findAll().stream()
                .filter(this::isVisible)
                .toList();
    }

    private List<AnalyticsEventRecord> customerEvents(String customerRef) {
        return analyticsEventRecordRepository.findAll().stream()
                .filter(event -> customerRef.equalsIgnoreCase(normalizeCustomerRef(event.getCustomerRef())))
                .sorted(Comparator.comparing(AnalyticsEventRecord::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Map<UUID, Double> buildAnchorWeights(List<AnalyticsEventRecord> events, boolean commerceIntentOnly, RecommendationSettings settings) {
        Map<UUID, Double> weights = new LinkedHashMap<>();
        for (AnalyticsEventRecord event : events) {
            UUID productId = parseUuid(event.getProductId());
            if (productId == null) {
                continue;
            }
            double signalWeight = signalWeight(event.getEventType(), commerceIntentOnly, settings);
            if (signalWeight <= 0) {
                continue;
            }
            weights.merge(productId, signalWeight, Double::sum);
        }
        return weights.entrySet().stream()
                .sorted(
                        Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder())
                                .thenComparing(entry -> entry.getKey().toString())
                )
                .limit(4)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private void boostCoPurchasedProducts(Map<UUID, Double> scores, Set<UUID> anchorIds, double multiplier) {
        if (anchorIds == null || anchorIds.isEmpty()) {
            return;
        }
        Map<UUID, List<OrderItem>> orderItemsByOrder = orderItemRepository.findAll().stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
        for (List<OrderItem> items : orderItemsByOrder.values()) {
            Set<UUID> orderedProductIds = items.stream()
                    .map(item -> item.getProduct().getId())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            boolean containsAnchor = orderedProductIds.stream().anyMatch(anchorIds::contains);
            if (!containsAnchor) {
                continue;
            }
            for (OrderItem item : items) {
                UUID candidateId = item.getProduct().getId();
                if (anchorIds.contains(candidateId) || !isVisible(item.getProduct())) {
                    continue;
                }
                scores.merge(candidateId, item.getQuantity() * multiplier, Double::sum);
            }
        }
    }

    private boolean isVisible(Product product) {
        return product != null && product.isActive() && product.getDeletedAt() == null;
    }

    private boolean sameCategory(Product left, Product right) {
        return left.getCategory() != null
                && right.getCategory() != null
                && Objects.equals(left.getCategory().getId(), right.getCategory().getId());
    }

    private boolean sameBrand(Product left, Product right) {
        return left.getBrand() != null
                && right.getBrand() != null
                && Objects.equals(left.getBrand().getId(), right.getBrand().getId());
    }

    private String imageUrl(Product product) {
        if (product.getAttributes() == null) {
            return null;
        }
        Object direct = product.getAttributes().get("imageUrl");
        if (direct == null) {
            direct = product.getAttributes().get("image_url");
        }
        return direct == null ? null : direct.toString();
    }

    private BigDecimal extractSalePrice(Product product) {
        if (product.getAttributes() == null) {
            return null;
        }
        Object raw = product.getAttributes().get("salePrice");
        if (raw == null) {
            raw = product.getAttributes().get("sale_price");
        }
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

    private double signalWeight(AnalyticsEventType eventType, boolean commerceIntentOnly, RecommendationSettings settings) {
        return switch (eventType) {
            case CHECKOUT_COMPLETED -> settings.getCheckoutWeight();
            case ADD_TO_CART -> settings.getAddToCartWeight();
            case PRODUCT_VIEW -> commerceIntentOnly ? 0D : settings.getProductViewWeight();
            default -> 0D;
        };
    }

    private RecommendationSettings currentSettings() {
        return recommendationSettingsRepository.findAll().stream().findFirst().orElseGet(RecommendationSettings::new);
    }

    private String normalizeCustomerRef(String customerRef) {
        return customerRef == null || customerRef.isBlank() ? null : customerRef.trim();
    }

    private int sanitizeLimit(int limit, RecommendationSettings settings) {
        int max = settings.getMaxRecommendations() <= 0 ? 12 : settings.getMaxRecommendations();
        return Math.max(1, Math.min(limit, max));
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
}
