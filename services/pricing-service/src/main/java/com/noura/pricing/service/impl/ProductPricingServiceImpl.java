package com.noura.pricing.service.impl;

import com.noura.pricing.domain.entity.PricingCurrency;
import com.noura.pricing.domain.entity.ProductPrice;
import com.noura.pricing.dto.price.BulkPriceResolutionResponse;
import com.noura.pricing.dto.price.PriceResolutionResponse;
import com.noura.pricing.dto.price.PriceUpsertRequest;
import com.noura.pricing.dto.price.ProductPriceResponse;
import com.noura.pricing.exception.NotFoundException;
import com.noura.pricing.exception.PricingOperationException;
import com.noura.pricing.repository.PricingCurrencyRepository;
import com.noura.pricing.repository.ProductPriceRepository;
import com.noura.pricing.service.ProductPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of {@link ProductPricingService}.
 *
 * <p>This service applies pricing resolution rules for scope precedence, active windows,
 * and deterministic tie-breaking.</p>
 */
@Service
@RequiredArgsConstructor
public class ProductPricingServiceImpl implements ProductPricingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final ProductPriceRepository productPriceRepository;
    private final PricingCurrencyRepository pricingCurrencyRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PriceResolutionResponse resolveProductPrice(
            UUID productId,
            String currencyCode,
            UUID storeId,
            String channelCode,
            Instant at
    ) {
        Instant resolveAt = resolveAt(at);
        String resolvedCurrency = resolveCurrencyOrDefault(currencyCode);
        List<ProductPrice> candidates = productPriceRepository.findByProductIdAndCurrencyCodeIgnoreCase(productId, resolvedCurrency);
        ProductPrice selected = selectBestCandidate(candidates, storeId, normalizeChannelCode(channelCode), resolveAt)
                .orElseThrow(() -> new NotFoundException(
                        "PRICE_NOT_FOUND",
                        "No active price found for product and requested scope"
                ));
        return toResolutionResponse(selected, resolveAt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public BulkPriceResolutionResponse resolveBulkPrices(
            List<UUID> productIds,
            String currencyCode,
            UUID storeId,
            String channelCode,
            Instant at
    ) {
        List<UUID> normalizedProductIds = normalizeProductIds(productIds);
        Instant resolveAt = resolveAt(at);
        String resolvedCurrency = resolveCurrencyOrDefault(currencyCode);
        String normalizedChannelCode = normalizeChannelCode(channelCode);

        List<ProductPrice> rows = productPriceRepository.findByProductIdInAndCurrencyCodeIgnoreCase(normalizedProductIds, resolvedCurrency);
        Map<UUID, List<ProductPrice>> grouped = groupByProduct(rows);

        List<PriceResolutionResponse> resolved = new ArrayList<>();
        List<UUID> missingProductIds = new ArrayList<>();
        for (UUID productId : normalizedProductIds) {
            Optional<ProductPrice> selected = selectBestCandidate(grouped.get(productId), storeId, normalizedChannelCode, resolveAt);
            if (selected.isPresent()) {
                resolved.add(toResolutionResponse(selected.get(), resolveAt));
            } else {
                missingProductIds.add(productId);
            }
        }
        return new BulkPriceResolutionResponse(resolved, missingProductIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<PriceResolutionResponse> activeSnapshot(
            List<UUID> productIds,
            String currencyCode,
            UUID storeId,
            String channelCode,
            Instant at
    ) {
        BulkPriceResolutionResponse bulk = resolveBulkPrices(productIds, currencyCode, storeId, channelCode, at);
        return bulk.prices();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProductPriceResponse upsertPrice(PriceUpsertRequest request, String actorUserId) {
        validateWindow(request.startsAt(), request.endsAt());

        String currencyCode = normalizeCurrencyCode(request.currencyCode());
        PricingCurrency currency = pricingCurrencyRepository.findById(currencyCode)
                .orElseThrow(() -> new PricingOperationException(
                        HttpStatus.BAD_REQUEST,
                        "UNSUPPORTED_CURRENCY",
                        "Currency is not supported: " + currencyCode
                ));
        if (!currency.isActive()) {
            throw new PricingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "CURRENCY_INACTIVE",
                    "Currency is inactive: " + currencyCode
            );
        }

        BigDecimal basePrice = normalizeNonNegativeMoney(request.basePrice(), "basePrice");
        BigDecimal compareAtPrice = normalizeNullableNonNegativeMoney(request.compareAtPrice(), "compareAtPrice");
        if (compareAtPrice != null && compareAtPrice.compareTo(basePrice) <= 0) {
            throw new PricingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "COMPARE_AT_INVALID",
                    "compareAtPrice must be greater than basePrice when provided"
            );
        }

        String channelCode = normalizeChannelCode(request.channelCode());
        Integer priority = request.priority() == null ? 0 : request.priority();
        boolean active = request.active() == null || request.active();
        String actor = normalizeActor(actorUserId);

        ProductPrice entity = productPriceRepository.findByNaturalKey(
                        request.productId(),
                        currencyCode,
                        request.storeId(),
                        channelCode,
                        request.startsAt(),
                        request.endsAt()
                )
                .orElseGet(ProductPrice::new);

        if (entity.getId() == null) {
            entity.setCreatedBy(actor);
        }
        entity.setProductId(request.productId());
        entity.setCurrencyCode(currencyCode);
        entity.setBasePrice(basePrice);
        entity.setCompareAtPrice(compareAtPrice);
        entity.setChannelCode(channelCode);
        entity.setStoreId(request.storeId());
        entity.setStartsAt(request.startsAt());
        entity.setEndsAt(request.endsAt());
        entity.setPriority(priority);
        entity.setActive(active);
        entity.setUpdatedBy(actor);

        ProductPrice saved = productPriceRepository.save(entity);
        return toProductPriceResponse(saved);
    }

    /**
     * Groups price rows by product ID.
     *
     * @param rows rows to group
     * @return grouped map
     */
    private Map<UUID, List<ProductPrice>> groupByProduct(List<ProductPrice> rows) {
        Map<UUID, List<ProductPrice>> grouped = new LinkedHashMap<>();
        for (ProductPrice row : rows) {
            grouped.computeIfAbsent(row.getProductId(), ignored -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    /**
     * Selects the best candidate according to scope and tie-breaker rules.
     *
     * @param candidates candidate rows
     * @param storeId requested store scope
     * @param channelCode requested channel scope
     * @param resolveAt resolution timestamp
     * @return selected candidate row when available
     */
    private Optional<ProductPrice> selectBestCandidate(
            Collection<ProductPrice> candidates,
            UUID storeId,
            String channelCode,
            Instant resolveAt
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(ProductPrice::isActive)
                .filter(candidate -> isActiveWindow(resolveAt, candidate.getStartsAt(), candidate.getEndsAt()))
                .filter(candidate -> isScopeApplicable(candidate, storeId, channelCode))
                .max(Comparator
                        .comparingInt((ProductPrice candidate) -> scopeRank(candidate, storeId, channelCode))
                        .thenComparingInt(ProductPrice::getPriority)
                        .thenComparing(
                                candidate -> candidate.getStartsAt() == null ? Instant.MIN : candidate.getStartsAt()
                        )
                        .thenComparing(
                                candidate -> candidate.getUpdatedAt() == null ? Instant.MIN : candidate.getUpdatedAt()
                        ));
    }

    /**
     * Determines whether the requested timestamp is inside the active window.
     *
     * @param now resolution timestamp
     * @param startsAt optional start bound
     * @param endsAt optional end bound
     * @return true when the row is active at the requested time
     */
    private boolean isActiveWindow(Instant now, Instant startsAt, Instant endsAt) {
        boolean afterStart = startsAt == null || !now.isBefore(startsAt);
        boolean beforeEnd = endsAt == null || !now.isAfter(endsAt);
        return afterStart && beforeEnd;
    }

    /**
     * Determines whether a row is applicable for requested store/channel scope.
     *
     * @param candidate candidate row
     * @param storeId requested store scope
     * @param channelCode requested channel scope
     * @return true when row is applicable to the request
     */
    private boolean isScopeApplicable(ProductPrice candidate, UUID storeId, String channelCode) {
        if (candidate.getStoreId() != null) {
            return storeId != null && Objects.equals(candidate.getStoreId(), storeId);
        }
        String candidateChannel = normalizeChannelCode(candidate.getChannelCode());
        if (candidateChannel != null) {
            return channelCode != null && candidateChannel.equals(channelCode);
        }
        return true;
    }

    /**
     * Computes scope precedence score.
     *
     * @param candidate candidate row
     * @param storeId requested store scope
     * @param channelCode requested channel scope
     * @return precedence rank (higher is better)
     */
    private int scopeRank(ProductPrice candidate, UUID storeId, String channelCode) {
        if (candidate.getStoreId() != null && storeId != null && Objects.equals(candidate.getStoreId(), storeId)) {
            return 300;
        }
        String candidateChannel = normalizeChannelCode(candidate.getChannelCode());
        if (candidateChannel != null && channelCode != null && candidateChannel.equals(channelCode)) {
            return 200;
        }
        return 100;
    }

    /**
     * Resolves requested timestamp or defaults to now.
     *
     * @param at optional timestamp
     * @return resolved timestamp
     */
    private Instant resolveAt(Instant at) {
        return at == null ? Instant.now() : at;
    }

    /**
     * Resolves currency code from request or default currency configuration.
     *
     * @param currencyCode optional request currency
     * @return normalized active currency code
     */
    private String resolveCurrencyOrDefault(String currencyCode) {
        if (currencyCode != null && !currencyCode.isBlank()) {
            String normalized = normalizeCurrencyCode(currencyCode);
            PricingCurrency configured = pricingCurrencyRepository.findById(normalized)
                    .orElseThrow(() -> new PricingOperationException(
                            HttpStatus.BAD_REQUEST,
                            "UNSUPPORTED_CURRENCY",
                            "Currency is not supported: " + normalized
                    ));
            if (!configured.isActive()) {
                throw new PricingOperationException(
                        HttpStatus.BAD_REQUEST,
                        "CURRENCY_INACTIVE",
                        "Currency is inactive: " + normalized
                );
            }
            return normalized;
        }
        return pricingCurrencyRepository.findByDefaultCurrencyTrueAndActiveTrue()
                .map(PricingCurrency::getCode)
                .orElse("USD");
    }

    /**
     * Normalizes product ID input and keeps deterministic order.
     *
     * @param productIds raw product IDs
     * @return normalized ordered unique product IDs
     */
    private List<UUID> normalizeProductIds(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new PricingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PRODUCT_IDS_REQUIRED",
                    "At least one productId must be provided"
            );
        }
        return new ArrayList<>(new LinkedHashSet<>(productIds));
    }

    /**
     * Validates active window bounds.
     *
     * @param startsAt optional start bound
     * @param endsAt optional end bound
     */
    private void validateWindow(Instant startsAt, Instant endsAt) {
        if (startsAt != null && endsAt != null && endsAt.isBefore(startsAt)) {
            throw new PricingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PRICE_WINDOW_INVALID",
                    "endsAt must be greater than or equal to startsAt"
            );
        }
    }

    /**
     * Normalizes currency code.
     *
     * @param value raw currency code
     * @return normalized currency code
     */
    private String normalizeCurrencyCode(String value) {
        if (value == null || value.isBlank()) {
            throw new PricingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "CURRENCY_REQUIRED",
                    "currencyCode is required"
            );
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            throw new PricingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "CURRENCY_INVALID",
                    "currencyCode must have exactly 3 characters"
            );
        }
        return normalized;
    }

    /**
     * Normalizes channel code.
     *
     * @param value raw channel code
     * @return normalized channel code or null
     */
    private String normalizeChannelCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes actor identity for audit fields.
     *
     * @param actor raw actor value
     * @return normalized actor value
     */
    private String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * Normalizes a mandatory non-negative money value to scale 4.
     *
     * @param value raw value
     * @param fieldName field name for validation messaging
     * @return normalized value
     */
    private BigDecimal normalizeNonNegativeMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new PricingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_" + fieldName.toUpperCase(Locale.ROOT),
                    fieldName + " is required"
            );
        }
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new PricingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_" + fieldName.toUpperCase(Locale.ROOT),
                    fieldName + " must be non-negative"
            );
        }
        return normalized;
    }

    /**
     * Normalizes an optional non-negative money value to scale 4.
     *
     * @param value raw value
     * @param fieldName field name for validation messaging
     * @return normalized value or null
     */
    private BigDecimal normalizeNullableNonNegativeMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            return null;
        }
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new PricingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_" + fieldName.toUpperCase(Locale.ROOT),
                    fieldName + " must be non-negative"
            );
        }
        return normalized;
    }

    /**
     * Converts a persisted row into resolution response.
     *
     * @param entity source row
     * @param resolvedAt resolution timestamp
     * @return resolution response
     */
    private PriceResolutionResponse toResolutionResponse(ProductPrice entity, Instant resolvedAt) {
        BigDecimal effectivePrice = normalizeMoney(entity.getBasePrice());
        BigDecimal compareAtPrice = sanitizeCompareAt(entity.getCompareAtPrice(), effectivePrice);
        return new PriceResolutionResponse(
                entity.getProductId(),
                entity.getCurrencyCode(),
                effectivePrice,
                compareAtPrice,
                effectivePrice,
                entity.getId(),
                entity.getChannelCode(),
                entity.getStoreId(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getPriority(),
                resolvedAt
        );
    }

    /**
     * Converts persisted row into admin response model.
     *
     * @param entity source row
     * @return admin response model
     */
    private ProductPriceResponse toProductPriceResponse(ProductPrice entity) {
        BigDecimal effectivePrice = normalizeMoney(entity.getBasePrice());
        BigDecimal compareAtPrice = sanitizeCompareAt(entity.getCompareAtPrice(), effectivePrice);
        return new ProductPriceResponse(
                entity.getId(),
                entity.getProductId(),
                entity.getCurrencyCode(),
                effectivePrice,
                compareAtPrice,
                effectivePrice,
                entity.getChannelCode(),
                entity.getStoreId(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getPriority(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Normalizes money value to scale 4.
     *
     * @param value raw value
     * @return normalized value
     */
    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Enforces compare-at visibility rule.
     *
     * <p>Compare-at is exposed only when strictly higher than effective sell price.</p>
     *
     * @param compareAtPrice raw compare-at price
     * @param effectivePrice effective price
     * @return sanitized compare-at price
     */
    private BigDecimal sanitizeCompareAt(BigDecimal compareAtPrice, BigDecimal effectivePrice) {
        if (compareAtPrice == null) {
            return null;
        }
        BigDecimal normalized = normalizeMoney(compareAtPrice);
        if (normalized.compareTo(effectivePrice) <= 0) {
            return null;
        }
        return normalized;
    }
}

