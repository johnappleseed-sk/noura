package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.*;
import com.noura.platform.domain.enums.PriceListType;
import com.noura.platform.domain.enums.PromotionApplicableEntityType;
import com.noura.platform.domain.enums.PromotionType;
import com.noura.platform.dto.pricing.*;
import com.noura.platform.repository.*;
import com.noura.platform.service.PricingCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PricingCatalogServiceImpl implements PricingCatalogService {

    private final PriceRepository priceRepository;
    private final PriceListRepository priceListRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionApplicationRepository promotionApplicationRepository;

    /**
     * Creates price list.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public PriceListDto createPriceList(PriceListRequest request) {
        if (priceListRepository.existsByNameIgnoreCase(request.name())) {
            throw new BadRequestException("PRICE_LIST_EXISTS", "Price list already exists");
        }
        PriceList priceList = new PriceList();
        priceList.setName(request.name().trim());
        priceList.setType(request.type());
        priceList.setCustomerGroupId(request.customerGroupId());
        priceList.setChannelId(request.channelId());
        PriceList saved = priceListRepository.save(priceList);
        return toPriceListDto(saved);
    }

    /**
     * Retrieves price lists.
     *
     * @return A list of matching items.
     */
    @Override
    public List<PriceListDto> priceLists() {
        return priceListRepository.findAll().stream()
                .sorted(Comparator.comparing(PriceList::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toPriceListDto)
                .toList();
    }

    /**
     * Upserts price.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public PriceDto upsertPrice(PriceUpsertRequest request) {
        ProductVariant variant = productVariantRepository.findById(request.variantId())
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Variant not found"));
        PriceList priceList = priceListRepository.findById(request.priceListId())
                .orElseThrow(() -> new NotFoundException("PRICE_LIST_NOT_FOUND", "Price list not found"));
        if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("PRICE_WINDOW_INVALID", "endDate must be after startDate");
        }
        String currency = request.currency().toUpperCase(Locale.ROOT);
        Price price = priceRepository.findByNaturalKey(
                        request.variantId(),
                        request.priceListId(),
                        currency,
                        request.startDate(),
                        request.endDate()
                )
                .orElseGet(Price::new);

        price.setVariant(variant);
        price.setPriceList(priceList);
        price.setAmount(request.amount());
        price.setCurrency(currency);
        price.setStartDate(request.startDate());
        price.setEndDate(request.endDate());
        price.setPriority(request.priority() == null ? 0 : request.priority());
        Price saved = priceRepository.save(price);
        return toPriceDto(saved);
    }

    /**
     * Quotes variant price.
     *
     * @param variantId The variant id used to locate the target record.
     * @param customerGroupId The customer group id used to locate the target record.
     * @param channelId The channel id used to locate the target record.
     * @return The mapped DTO representation.
     */
    @Override
    public PriceQuoteDto quoteVariantPrice(UUID variantId, UUID customerGroupId, UUID channelId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Variant not found"));
        Instant now = Instant.now();
        List<Price> prices = priceRepository.findByVariantId(variantId).stream()
                .filter(price -> isActive(now, price.getStartDate(), price.getEndDate()))
                .toList();
        BigDecimal baseAmount = variant.getPriceOverride() != null
                ? variant.getPriceOverride()
                : variant.getProduct().getBasePrice();
        String currency = "USD";

        Optional<Price> basePrice = prices.stream()
                .filter(price -> price.getPriceList().getType() == PriceListType.BASE)
                .max(Comparator.comparingInt(Price::getPriority));
        if (basePrice.isPresent()) {
            baseAmount = basePrice.get().getAmount();
            currency = basePrice.get().getCurrency();
        }

        Optional<Price> override = prices.stream()
                .filter(price -> matchesScope(price.getPriceList(), customerGroupId, channelId))
                .max(Comparator.comparingInt(Price::getPriority));
        BigDecimal effectiveAmount = override.map(Price::getAmount).orElse(baseAmount);
        if (override.isPresent()) {
            currency = override.get().getCurrency();
        }

        List<Promotion> activePromotions = activeApplicablePromotions(now, variant);
        List<UUID> appliedPromotionIds = new ArrayList<>();
        BigDecimal finalAmount = effectiveAmount;
        for (Promotion promotion : activePromotions) {
            BigDecimal next = applyPromotion(finalAmount, promotion);
            if (next.compareTo(finalAmount) < 0) {
                appliedPromotionIds.add(promotion.getId());
                finalAmount = next;
            }
        }
        return new PriceQuoteDto(variantId, currency, effectiveAmount, finalAmount, appliedPromotionIds);
    }

    /**
     * Creates promotion.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public PromotionDto createPromotion(PromotionCreateRequest request) {
        validatePromotionWindow(request.startDate(), request.endDate());
        String normalizedCode = trimToNull(request.code());
        if (normalizedCode != null) {
            promotionRepository.findByCodeIgnoreCase(normalizedCode).ifPresent(existing -> {
                throw new BadRequestException("PROMOTION_CODE_EXISTS", "Promotion code already exists");
            });
        }
        Promotion promotion = new Promotion();
        promotion.setName(request.name());
        promotion.setCode(normalizedCode);
        promotion.setDescription(trimToNull(request.description()));
        promotion.setType(request.type());
        promotion.setCouponCode(request.couponCode());
        promotion.setConditions(request.conditions() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.conditions()));
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        promotion.setActive(request.active() == null || request.active());
        promotion.setStackable(request.stackable() == null || request.stackable());
        promotion.setPriority(request.priority() == null ? 0 : request.priority());
        promotion.setUsageLimitTotal(request.usageLimitTotal());
        promotion.setUsageLimitPerCustomer(request.usageLimitPerCustomer());
        promotion.setUsageCount(0);
        promotion.setCustomerSegment(trimToNull(request.customerSegment()));
        promotion.setArchived(false);
        Promotion saved = promotionRepository.save(promotion);

        if (request.applications() != null) {
            for (PromotionApplicationItemRequest item : request.applications()) {
                PromotionApplication mapping = new PromotionApplication();
                mapping.setPromotion(saved);
                mapping.setApplicableEntityType(item.applicableEntityType());
                mapping.setApplicableEntityId(item.applicableEntityId());
                promotionApplicationRepository.save(mapping);
            }
        }
        return toPromotionDto(saved);
    }

    /**
     * Retrieves active promotions.
     *
     * @return A list of matching items.
     */
    @Override
    public List<PromotionDto> activePromotions() {
        Instant now = Instant.now();
        return promotionRepository.findByActiveTrueAndArchivedFalse().stream()
                .filter(promotion -> isActive(now, promotion.getStartDate(), promotion.getEndDate()))
                .sorted(Comparator.comparingInt(Promotion::getPriority).reversed())
                .map(this::toPromotionDto)
                .toList();
    }

    /**
     * Retrieves to price dto.
     *
     * @param entity The source object to transform.
     * @return The result of to price dto.
     */
    private PriceDto toPriceDto(Price entity) {
        return new PriceDto(
                entity.getId(),
                entity.getVariant().getId(),
                entity.getPriceList().getId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getPriority()
        );
    }

    /**
     * Retrieves to price list dto.
     *
     * @param entity The source object to transform.
     * @return The result of to price list dto.
     */
    private PriceListDto toPriceListDto(PriceList entity) {
        return new PriceListDto(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getCustomerGroupId(),
                entity.getChannelId()
        );
    }

    /**
     * Retrieves to promotion dto.
     *
     * @param promotion The promotion value.
     * @return The result of to promotion dto.
     */
    private PromotionDto toPromotionDto(Promotion promotion) {
        List<PromotionApplicationItemDto> mappings = promotionApplicationRepository.findByPromotionId(promotion.getId()).stream()
                .map(item -> new PromotionApplicationItemDto(item.getApplicableEntityType(), item.getApplicableEntityId()))
                .toList();
        return new PromotionDto(
                promotion.getId(),
                promotion.getName(),
                promotion.getCode(),
                promotion.getDescription(),
                promotion.getType(),
                promotion.getCouponCode(),
                promotion.getConditions(),
                promotion.getStartDate(),
                promotion.getEndDate(),
                promotion.isActive(),
                promotion.isStackable(),
                promotion.getPriority(),
                promotion.getUsageLimitTotal(),
                promotion.getUsageLimitPerCustomer(),
                promotion.getUsageCount(),
                promotion.getCustomerSegment(),
                promotion.isArchived(),
                readDecimal(promotion.getConditions(), "percent"),
                resolveDiscountAmount(promotion),
                mappings
        );
    }

    /**
     * Determines whether matches scope.
     *
     * @param list The list value.
     * @param customerGroupId The customer group id used to locate the target record.
     * @param channelId The channel id used to locate the target record.
     * @return True when the condition is satisfied; otherwise false.
     */
    private boolean matchesScope(PriceList list, UUID customerGroupId, UUID channelId) {
        if (list.getType() == PriceListType.BASE) {
            return true;
        }
        if (list.getType() == PriceListType.GROUP) {
            return Objects.equals(list.getCustomerGroupId(), customerGroupId);
        }
        if (list.getType() == PriceListType.CHANNEL) {
            return Objects.equals(list.getChannelId(), channelId);
        }
        return true;
    }

    /**
     * Retrieves active applicable promotions.
     *
     * @param now The now value.
     * @param variant The variant value.
     * @return The result of active applicable promotions.
     */
    private List<Promotion> activeApplicablePromotions(Instant now, ProductVariant variant) {
        UUID variantId = variant.getId();
        UUID productId = variant.getProduct().getId();
        UUID categoryId = variant.getProduct().getCategory() == null ? null : variant.getProduct().getCategory().getId();

        Set<UUID> applicableIds = new LinkedHashSet<>();
        applicableIds.addAll(
                promotionApplicationRepository.findByApplicableEntityTypeAndApplicableEntityId(
                                PromotionApplicableEntityType.VARIANT,
                                variantId
                        ).stream()
                        .map(item -> item.getPromotion().getId())
                        .toList()
        );
        applicableIds.addAll(
                promotionApplicationRepository.findByApplicableEntityTypeAndApplicableEntityId(
                                PromotionApplicableEntityType.PRODUCT,
                                productId
                        ).stream()
                        .map(item -> item.getPromotion().getId())
                        .toList()
        );
        if (categoryId != null) {
            applicableIds.addAll(
                    promotionApplicationRepository.findByApplicableEntityTypeAndApplicableEntityId(
                                    PromotionApplicableEntityType.CATEGORY,
                                    categoryId
                            ).stream()
                            .map(item -> item.getPromotion().getId())
                            .toList()
            );
        }
        return applicableIds.stream()
                .map(promotionId -> promotionRepository.findById(promotionId).orElse(null))
                .filter(Objects::nonNull)
                .filter(Promotion::isActive)
                .filter(promotion -> isActive(now, promotion.getStartDate(), promotion.getEndDate()))
                .sorted(Comparator.comparingInt(Promotion::getPriority).reversed())
                .toList();
    }

    /**
     * Executes apply promotion.
     *
     * @param amount The amount value.
     * @param promotion The promotion value.
     * @return The result of apply promotion.
     */
    private BigDecimal applyPromotion(BigDecimal amount, Promotion promotion) {
        Map<String, Object> conditions = promotion.getConditions();
        if (promotion.getType() == PromotionType.PERCENTAGE) {
            Object percentObj = conditions.get("percent");
            if (percentObj instanceof Number number) {
                BigDecimal percent = BigDecimal.valueOf(number.doubleValue());
                BigDecimal discount = amount.multiply(percent).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                BigDecimal next = amount.subtract(discount);
                return next.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : next;
            }
        }
        if (promotion.getType() == PromotionType.FIXED) {
            Object fixedObj = conditions.get("amount");
            if (fixedObj instanceof Number number) {
                BigDecimal discount = BigDecimal.valueOf(number.doubleValue());
                BigDecimal next = amount.subtract(discount);
                return next.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : next;
            }
        }
        if (promotion.getType() == PromotionType.CART_THRESHOLD_DISCOUNT) {
            BigDecimal threshold = readDecimal(conditions, "threshold");
            if (threshold.signum() > 0 && amount.compareTo(threshold) < 0) {
                return amount;
            }
            BigDecimal percent = readDecimal(conditions, "percent");
            if (percent.signum() > 0) {
                return applyPromotion(amount, fallbackPromotion(PromotionType.PERCENTAGE, percent));
            }
            BigDecimal fixed = readDecimal(conditions, "amount");
            if (fixed.signum() > 0) {
                return applyPromotion(amount, fallbackPromotion(PromotionType.FIXED, fixed));
            }
        }
        return amount;
    }

    private Promotion fallbackPromotion(PromotionType type, BigDecimal value) {
        Promotion promotion = new Promotion();
        promotion.setType(type);
        Map<String, Object> conditions = new LinkedHashMap<>();
        if (type == PromotionType.PERCENTAGE) {
            conditions.put("percent", value);
        } else {
            conditions.put("amount", value);
        }
        promotion.setConditions(conditions);
        return promotion;
    }

    /**
     * Determines whether is active.
     *
     * @param now The now value.
     * @param start The start value.
     * @param end The end value.
     * @return True when the condition is satisfied; otherwise false.
     */
    private boolean isActive(Instant now, Instant start, Instant end) {
        boolean afterStart = start == null || !now.isBefore(start);
        boolean beforeEnd = end == null || !now.isAfter(end);
        return afterStart && beforeEnd;
    }

    private void validatePromotionWindow(Instant start, Instant end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("PROMOTION_WINDOW_INVALID", "endDate must be after startDate");
        }
    }

    private BigDecimal readDecimal(Map<String, Object> conditions, String key) {
        if (conditions == null) {
            return BigDecimal.ZERO;
        }
        Object raw = conditions.get(key);
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (raw instanceof String string && !string.isBlank()) {
            try {
                return new BigDecimal(string.trim());
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveDiscountAmount(Promotion promotion) {
        BigDecimal directAmount = readDecimal(promotion.getConditions(), "amount");
        if (directAmount.signum() > 0) {
            return directAmount;
        }
        BigDecimal bundleAmount = readDecimal(promotion.getConditions(), "bundleAmount");
        return bundleAmount.signum() > 0 ? bundleAmount : null;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
