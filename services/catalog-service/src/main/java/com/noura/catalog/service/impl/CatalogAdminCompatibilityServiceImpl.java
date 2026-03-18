package com.noura.catalog.service.impl;

import com.noura.catalog.dto.admin.MerchandisingBoostRequest;
import com.noura.catalog.dto.admin.MerchandisingBoostResponse;
import com.noura.catalog.dto.admin.MerchandisingPreviewResponse;
import com.noura.catalog.dto.admin.MerchandisingProductDto;
import com.noura.catalog.dto.admin.MerchandisingSettingsResponse;
import com.noura.catalog.dto.admin.MerchandisingSettingsUpdateRequest;
import com.noura.catalog.dto.admin.RecommendationAdminPreviewResponse;
import com.noura.catalog.dto.admin.RecommendationAdminProductDto;
import com.noura.catalog.dto.admin.RecommendationProductPreviewResponse;
import com.noura.catalog.dto.admin.RecommendationSettingsResponse;
import com.noura.catalog.dto.admin.RecommendationSettingsUpdateRequest;
import com.noura.catalog.dto.product.ProductDto;
import com.noura.catalog.dto.product.ProductMediaDto;
import com.noura.catalog.dto.product.ProductStoreInventoryDto;
import com.noura.catalog.exception.NotFoundException;
import com.noura.catalog.service.CatalogAdminCompatibilityService;
import com.noura.catalog.service.CatalogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Deterministic, in-process compatibility implementation for admin recommendation and
 * merchandising controls.
 *
 * <p>This service intentionally avoids introducing a new cross-service dependency just to keep
 * legacy admin screens operational. The backing state is explicitly lightweight and scoped to
 * compatibility controls until a dedicated admin configuration store is extracted.</p>
 */
@Service
@RequiredArgsConstructor
public class CatalogAdminCompatibilityServiceImpl implements CatalogAdminCompatibilityService {

    private static final RecommendationSettingsResponse DEFAULT_RECOMMENDATION_SETTINGS =
            new RecommendationSettingsResponse(
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    1.0,
                    4.0,
                    8.0,
                    30.0,
                    20.0,
                    5.0,
                    6.0,
                    3.0,
                    5.0,
                    60.0,
                    12
            );

    private static final MerchandisingSettingsResponse DEFAULT_MERCHANDISING_SETTINGS =
            new MerchandisingSettingsResponse(
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    1.0,
                    0.5,
                    0.75,
                    4.0,
                    0.6,
                    1.0,
                    30,
                    25.0,
                    20.0,
                    15.0,
                    20.0,
                    48
            );

    private final CatalogQueryService catalogQueryService;

    private final AtomicReference<RecommendationSettingsResponse> recommendationSettings =
            new AtomicReference<>(DEFAULT_RECOMMENDATION_SETTINGS);
    private final AtomicReference<MerchandisingSettingsResponse> merchandisingSettings =
            new AtomicReference<>(DEFAULT_MERCHANDISING_SETTINGS);
    private final Map<UUID, MerchandisingBoostState> merchandisingBoosts = new ConcurrentHashMap<>();

    @Override
    public RecommendationSettingsResponse getRecommendationSettings() {
        return recommendationSettings.get();
    }

    @Override
    public RecommendationSettingsResponse updateRecommendationSettings(
            RecommendationSettingsUpdateRequest request,
            String actorUserId
    ) {
        RecommendationSettingsResponse updated = new RecommendationSettingsResponse(
                recommendationSettings.get().id(),
                request.productViewWeight(),
                request.addToCartWeight(),
                request.checkoutWeight(),
                request.trendingBoost(),
                request.bestSellerBoost(),
                request.ratingWeight(),
                request.categoryAffinityWeight(),
                request.brandAffinityWeight(),
                request.coPurchaseWeight(),
                request.dealBoost(),
                request.maxRecommendations()
        );
        recommendationSettings.set(updated);
        return updated;
    }

    @Override
    public RecommendationAdminPreviewResponse previewRecommendations(String customerRef, UUID productId, int limit) {
        RecommendationSettingsResponse settings = recommendationSettings.get();
        int boundedLimit = Math.min(Math.max(limit, 1), settings.maxRecommendations());

        List<RecommendationAdminProductDto> trending = mapRecommendationProducts(
                catalogQueryService.trendingRecommendations(boundedLimit),
                product -> scoreTrending(product, settings),
                "Trending catalog signal"
        );
        List<RecommendationAdminProductDto> bestSellers = mapRecommendationProducts(
                catalogQueryService.bestSellerRecommendations(boundedLimit),
                product -> scoreBestSeller(product, settings),
                "Best-seller signal"
        );
        List<RecommendationAdminProductDto> deals = mapRecommendationProducts(
                catalogQueryService.dealRecommendations(boundedLimit),
                product -> scoreDeal(product, settings),
                "Deal boost"
        );
        List<RecommendationAdminProductDto> personalized = mapRecommendationProducts(
                catalogQueryService.personalizedRecommendations(customerRef, boundedLimit),
                product -> scorePersonalized(product, settings),
                customerRef == null || customerRef.isBlank()
                        ? "Anonymous personalization fallback"
                        : "Customer affinity preview"
        );
        List<RecommendationAdminProductDto> crossSell = mapRecommendationProducts(
                catalogQueryService.crossSellRecommendations(customerRef, boundedLimit),
                product -> scoreCrossSell(product, settings),
                customerRef == null || customerRef.isBlank()
                        ? "Anonymous cross-sell fallback"
                        : "Cross-sell affinity preview"
        );

        RecommendationProductPreviewResponse productPreview = productId == null
                ? new RecommendationProductPreviewResponse(null, List.of(), List.of())
                : new RecommendationProductPreviewResponse(
                productId,
                mapRecommendationProducts(
                        catalogQueryService.relatedProducts(productId, boundedLimit),
                        product -> scoreProductNeighbor(product, settings),
                        "Category and brand neighbor"
                ),
                mapRecommendationProducts(
                        catalogQueryService.frequentlyBoughtTogether(productId, boundedLimit),
                        product -> scoreCoPurchase(product, settings),
                        "Frequently bought together"
                )
        );

        return new RecommendationAdminPreviewResponse(
                settings,
                normalize(customerRef),
                productId,
                trending,
                bestSellers,
                deals,
                personalized,
                crossSell,
                productPreview
        );
    }

    @Override
    public MerchandisingSettingsResponse getMerchandisingSettings() {
        return merchandisingSettings.get();
    }

    @Override
    public MerchandisingSettingsResponse updateMerchandisingSettings(
            MerchandisingSettingsUpdateRequest request,
            String actorUserId
    ) {
        MerchandisingSettingsResponse updated = new MerchandisingSettingsResponse(
                merchandisingSettings.get().id(),
                request.popularityWeight(),
                request.inventoryWeight(),
                request.impressionWeight(),
                request.clickWeight(),
                request.clickThroughRateWeight(),
                request.manualBoostWeight(),
                request.newArrivalWindowDays(),
                request.newArrivalBoost(),
                request.trendingBoost(),
                request.bestSellerBoost(),
                request.lowStockPenalty(),
                request.maxPageSize()
        );
        merchandisingSettings.set(updated);
        return updated;
    }

    @Override
    public List<MerchandisingBoostResponse> listMerchandisingBoosts() {
        return merchandisingBoosts.values().stream()
                .sorted(Comparator.comparing(MerchandisingBoostState::createdAt).reversed())
                .map(this::toBoostResponse)
                .toList();
    }

    @Override
    public MerchandisingBoostResponse createMerchandisingBoost(MerchandisingBoostRequest request, String actorUserId) {
        ProductDto product = requireProduct(request.productId());
        UUID boostId = UUID.randomUUID();
        MerchandisingBoostState state = new MerchandisingBoostState(
                boostId,
                product.id(),
                product.name(),
                request.label().trim(),
                request.boostValue(),
                request.active(),
                request.startAt(),
                request.endAt(),
                Instant.now()
        );
        merchandisingBoosts.put(boostId, state);
        return toBoostResponse(state);
    }

    @Override
    public MerchandisingBoostResponse updateMerchandisingBoost(
            UUID boostId,
            MerchandisingBoostRequest request,
            String actorUserId
    ) {
        MerchandisingBoostState existing = requireBoost(boostId);
        ProductDto product = requireProduct(request.productId());
        MerchandisingBoostState updated = new MerchandisingBoostState(
                existing.id(),
                product.id(),
                product.name(),
                request.label().trim(),
                request.boostValue(),
                request.active(),
                request.startAt(),
                request.endAt(),
                existing.createdAt()
        );
        merchandisingBoosts.put(boostId, updated);
        return toBoostResponse(updated);
    }

    @Override
    public void deleteMerchandisingBoost(UUID boostId) {
        requireBoost(boostId);
        merchandisingBoosts.remove(boostId);
    }

    @Override
    public MerchandisingPreviewResponse previewMerchandising(String query, UUID categoryId, UUID storeId, int limit) {
        MerchandisingSettingsResponse settings = merchandisingSettings.get();
        int boundedLimit = Math.min(Math.max(limit, 1), settings.maxPageSize());

        List<ProductDto> candidateProducts = catalogQueryService.listProducts(
                normalize(query),
                null,
                categoryId,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, Math.max(boundedLimit * 3, boundedLimit), Sort.by(Sort.Direction.DESC, "updatedAt"))
        ).getContent().stream()
                .filter(product -> storeId == null || isAvailableInStore(product, storeId))
                .toList();

        List<MerchandisingBoostResponse> activeBoosts = listMerchandisingBoosts().stream()
                .filter(this::isBoostCurrentlyActive)
                .toList();
        Map<UUID, Double> activeBoostValues = activeBoosts.stream()
                .collect(Collectors.toMap(
                        MerchandisingBoostResponse::productId,
                        MerchandisingBoostResponse::boostValue,
                        Double::sum
                ));

        List<MerchandisingProductDto> ranked = candidateProducts.stream()
                .map(product -> toMerchandisingProduct(product, settings, activeBoostValues))
                .sorted(Comparator.comparingDouble(MerchandisingProductDto::merchandisingScore).reversed())
                .toList();

        List<MerchandisingProductDto> popularity = candidateProducts.stream()
                .map(product -> toMerchandisingProduct(product, settings, activeBoostValues))
                .sorted(Comparator.comparingInt(MerchandisingProductDto::stockQty).reversed()
                        .thenComparing(MerchandisingProductDto::name))
                .limit(boundedLimit)
                .toList();

        List<MerchandisingProductDto> trending = catalogQueryService.trendingRecommendations(boundedLimit * 2).stream()
                .filter(product -> categoryId == null || categoryId.equals(resolveCategoryId(product)))
                .filter(product -> storeId == null || isAvailableInStore(product, storeId))
                .map(product -> toMerchandisingProduct(product, settings, activeBoostValues))
                .limit(boundedLimit)
                .toList();

        List<MerchandisingProductDto> bestSelling = catalogQueryService.bestSellerRecommendations(boundedLimit * 2).stream()
                .filter(product -> categoryId == null || categoryId.equals(resolveCategoryId(product)))
                .filter(product -> storeId == null || isAvailableInStore(product, storeId))
                .map(product -> toMerchandisingProduct(product, settings, activeBoostValues))
                .limit(boundedLimit)
                .toList();

        List<MerchandisingProductDto> newest = candidateProducts.stream()
                .map(product -> toMerchandisingProduct(product, settings, activeBoostValues))
                .sorted(Comparator.comparing(MerchandisingProductDto::isNew).reversed()
                        .thenComparingDouble(MerchandisingProductDto::merchandisingScore).reversed())
                .limit(boundedLimit)
                .toList();

        return new MerchandisingPreviewResponse(
                settings,
                categoryId,
                storeId,
                normalize(query),
                ranked.stream().limit(boundedLimit).toList(),
                popularity,
                trending,
                bestSelling,
                newest,
                activeBoosts
        );
    }

    private List<RecommendationAdminProductDto> mapRecommendationProducts(
            List<ProductDto> products,
            Function<ProductDto, Double> scoreFunction,
            String reason
    ) {
        return products.stream()
                .map(product -> new RecommendationAdminProductDto(
                        product.id(),
                        product.name(),
                        product.category(),
                        round(scoreFunction.apply(product)),
                        reason
                ))
                .toList();
    }

    private ProductDto requireProduct(UUID productId) {
        try {
            return catalogQueryService.getProduct(productId);
        } catch (NotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
        }
    }

    private MerchandisingBoostState requireBoost(UUID boostId) {
        MerchandisingBoostState state = merchandisingBoosts.get(boostId);
        if (state == null) {
            throw new NotFoundException("MERCHANDISING_BOOST_NOT_FOUND", "Merchandising boost not found");
        }
        return state;
    }

    private MerchandisingBoostResponse toBoostResponse(MerchandisingBoostState state) {
        return new MerchandisingBoostResponse(
                state.id(),
                state.productId(),
                state.productName(),
                state.label(),
                state.boostValue(),
                state.active(),
                state.startAt(),
                state.endAt()
        );
    }

    private MerchandisingProductDto toMerchandisingProduct(
            ProductDto product,
            MerchandisingSettingsResponse settings,
            Map<UUID, Double> activeBoostValues
    ) {
        int stockQty = product.storeInventory().stream()
                .filter(ProductStoreInventoryDto::visible)
                .mapToInt(ProductStoreInventoryDto::stock)
                .sum();
        boolean isNew = product.reviewCount() <= 3 && !product.bestSeller();
        boolean lowStock = stockQty > 0 && stockQty < 5;
        double boostValue = activeBoostValues.getOrDefault(product.id(), 0.0d);
        double score = (product.popularityScore() * settings.popularityWeight())
                + (stockQty * settings.inventoryWeight())
                + (product.reviewCount() * settings.impressionWeight())
                + (product.averageRating() * settings.clickWeight())
                + (product.reviewCount() == 0 ? settings.clickThroughRateWeight() : 0.0d)
                + (boostValue * settings.manualBoostWeight())
                + (isNew ? settings.newArrivalBoost() : 0.0d)
                + (product.trending() ? settings.trendingBoost() : 0.0d)
                + (product.bestSeller() ? settings.bestSellerBoost() : 0.0d)
                - (lowStock ? settings.lowStockPenalty() : 0.0d);

        return new MerchandisingProductDto(
                product.id(),
                product.name(),
                resolveCategoryId(product),
                product.category(),
                product.price(),
                null,
                firstImageUrl(product.media()),
                stockQty,
                lowStock,
                product.allowBackorder(),
                isNew,
                product.trending(),
                product.bestSeller(),
                round(score)
        );
    }

    private boolean isAvailableInStore(ProductDto product, UUID storeId) {
        return product.storeInventory().stream()
                .anyMatch(inventory -> storeId.equals(inventory.storeId()) && inventory.visible() && inventory.published());
    }

    private UUID resolveCategoryId(ProductDto product) {
        Object categoryId = product.attributes() == null ? null : product.attributes().get("categoryId");
        if (categoryId instanceof UUID uuid) {
            return uuid;
        }
        if (categoryId instanceof String text) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isBoostCurrentlyActive(MerchandisingBoostResponse boost) {
        Instant now = Instant.now();
        if (!boost.active()) {
            return false;
        }
        if (boost.startAt() != null && now.isBefore(boost.startAt())) {
            return false;
        }
        if (boost.endAt() != null && now.isAfter(boost.endAt())) {
            return false;
        }
        return true;
    }

    private double scoreTrending(ProductDto product, RecommendationSettingsResponse settings) {
        return (product.trending() ? settings.trendingBoost() : 0.0d)
                + (product.popularityScore() * settings.productViewWeight())
                + (product.averageRating() * settings.ratingWeight());
    }

    private double scoreBestSeller(ProductDto product, RecommendationSettingsResponse settings) {
        return (product.bestSeller() ? settings.bestSellerBoost() : 0.0d)
                + (product.popularityScore() * settings.addToCartWeight())
                + (product.reviewCount() * settings.ratingWeight());
    }

    private double scoreDeal(ProductDto product, RecommendationSettingsResponse settings) {
        return (product.flashSale() ? settings.dealBoost() : 0.0d)
                + (product.popularityScore() * settings.productViewWeight())
                + (product.averageRating() * settings.ratingWeight());
    }

    private double scorePersonalized(ProductDto product, RecommendationSettingsResponse settings) {
        return (product.averageRating() * settings.ratingWeight())
                + (product.popularityScore() * settings.categoryAffinityWeight())
                + (product.bestSeller() ? settings.brandAffinityWeight() : 0.0d);
    }

    private double scoreCrossSell(ProductDto product, RecommendationSettingsResponse settings) {
        return (product.trending() ? settings.trendingBoost() : 0.0d)
                + (product.reviewCount() * settings.coPurchaseWeight())
                + (product.flashSale() ? settings.dealBoost() * 0.25d : 0.0d);
    }

    private double scoreProductNeighbor(ProductDto product, RecommendationSettingsResponse settings) {
        return (product.averageRating() * settings.ratingWeight())
                + (product.popularityScore() * settings.categoryAffinityWeight())
                + (product.bestSeller() ? settings.bestSellerBoost() : 0.0d);
    }

    private double scoreCoPurchase(ProductDto product, RecommendationSettingsResponse settings) {
        return (product.reviewCount() * settings.coPurchaseWeight())
                + (product.flashSale() ? settings.dealBoost() * 0.5d : 0.0d)
                + (product.popularityScore() * settings.checkoutWeight());
    }

    private String firstImageUrl(List<ProductMediaDto> media) {
        if (media == null || media.isEmpty()) {
            return null;
        }
        return media.stream()
                .sorted(Comparator.comparing(ProductMediaDto::primary).reversed()
                        .thenComparingInt(ProductMediaDto::sortOrder))
                .map(ProductMediaDto::url)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private record MerchandisingBoostState(
            UUID id,
            UUID productId,
            String productName,
            String label,
            double boostValue,
            boolean active,
            Instant startAt,
            Instant endAt,
            Instant createdAt
    ) {
    }
}
