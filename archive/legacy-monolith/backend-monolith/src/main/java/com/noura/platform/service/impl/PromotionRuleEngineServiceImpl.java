package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.Promotion;
import com.noura.platform.domain.entity.PromotionApplication;
import com.noura.platform.domain.enums.PromotionApplicableEntityType;
import com.noura.platform.domain.enums.PromotionType;
import com.noura.platform.dto.pricing.PromotionEvaluationDto;
import com.noura.platform.dto.pricing.PromotionEvaluationItemRequest;
import com.noura.platform.repository.PromotionApplicationRepository;
import com.noura.platform.repository.PromotionRepository;
import com.noura.platform.service.PromotionRuleEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionRuleEngineServiceImpl implements PromotionRuleEngineService {

    private final PromotionRepository promotionRepository;
    private final PromotionApplicationRepository promotionApplicationRepository;

    @Override
    @Transactional(readOnly = true)
    public PromotionEvaluationDto evaluate(BigDecimal subtotal,
                                           String couponCode,
                                           String customerSegment,
                                           List<PromotionEvaluationItemRequest> items) {
        BigDecimal effectiveSubtotal = subtotal != null ? subtotal : computeSubtotal(items);
        Instant now = Instant.now();
        String normalizedCoupon = trimToNull(couponCode);
        String normalizedSegment = normalizeToken(customerSegment);
        List<Promotion> activePromotions = promotionRepository.findByActiveTrueAndArchivedFalse().stream()
                .filter(promotion -> isWithinWindow(now, promotion.getStartDate(), promotion.getEndDate()))
                .filter(promotion -> usageAvailable(promotion))
                .sorted((left, right) -> Integer.compare(right.getPriority(), left.getPriority()))
                .toList();

        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean freeShipping = false;
        List<UUID> appliedPromotionIds = new ArrayList<>();
        List<String> appliedPromotionCodes = new ArrayList<>();

        for (Promotion promotion : activePromotions) {
            if (!matchesCoupon(promotion, normalizedCoupon)) {
                continue;
            }
            if (!matchesSegment(promotion, normalizedSegment)) {
                continue;
            }
            if (!matchesApplications(promotion, items)) {
                continue;
            }

            BigDecimal nextDiscount = calculateDiscount(promotion, effectiveSubtotal, items);
            boolean nextFreeShipping = promotion.getType() == PromotionType.FREE_SHIPPING
                    && meetsThreshold(promotion.getConditions(), effectiveSubtotal);
            if (nextDiscount.signum() <= 0 && !nextFreeShipping) {
                continue;
            }

            discountAmount = discountAmount.add(nextDiscount).min(effectiveSubtotal);
            freeShipping = freeShipping || nextFreeShipping;
            appliedPromotionIds.add(promotion.getId());
            appliedPromotionCodes.add(resolveDisplayCode(promotion));

            // Non-stackable promotions short-circuit the chain once they have been applied.
            if (!promotion.isStackable()) {
                break;
            }
        }

        return new PromotionEvaluationDto(
                effectiveSubtotal,
                discountAmount.setScale(2, RoundingMode.HALF_UP),
                freeShipping,
                appliedPromotionIds,
                appliedPromotionCodes
        );
    }

    private BigDecimal computeSubtotal(List<PromotionEvaluationItemRequest> items) {
        if (items == null) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean usageAvailable(Promotion promotion) {
        return promotion.getUsageLimitTotal() == null || promotion.getUsageCount() < promotion.getUsageLimitTotal();
    }

    private boolean matchesCoupon(Promotion promotion, String couponCode) {
        String promotionCoupon = trimToNull(promotion.getCouponCode());
        if (promotionCoupon == null) {
            return true;
        }
        return promotionCoupon.equalsIgnoreCase(couponCode);
    }

    private boolean matchesSegment(Promotion promotion, String customerSegment) {
        String promotionSegment = normalizeToken(promotion.getCustomerSegment());
        if (promotionSegment == null) {
            return true;
        }
        return Objects.equals(promotionSegment, customerSegment);
    }

    private boolean matchesApplications(Promotion promotion, List<PromotionEvaluationItemRequest> items) {
        List<PromotionApplication> applications = promotionApplicationRepository.findByPromotionId(promotion.getId());
        if (applications.isEmpty()) {
            return true;
        }
        Set<UUID> productIds = items == null ? Set.of() : items.stream()
                .map(PromotionEvaluationItemRequest::productId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> categoryIds = items == null ? Set.of() : items.stream()
                .map(PromotionEvaluationItemRequest::categoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (PromotionApplication application : applications) {
            if (application.getApplicableEntityType() == PromotionApplicableEntityType.PRODUCT && productIds.contains(application.getApplicableEntityId())) {
                return true;
            }
            if (application.getApplicableEntityType() == PromotionApplicableEntityType.CATEGORY && categoryIds.contains(application.getApplicableEntityId())) {
                return true;
            }
            if (application.getApplicableEntityType() == PromotionApplicableEntityType.COLLECTION && collectionMatch(promotion.getConditions(), application.getApplicableEntityId(), productIds)) {
                return true;
            }
        }
        return false;
    }

    private boolean collectionMatch(Map<String, Object> conditions, UUID applicableCollectionId, Set<UUID> productIds) {
        Object rawCollections = conditions == null ? null : conditions.get("collectionProductIds");
        if (!(rawCollections instanceof List<?> list)) {
            return false;
        }
        Set<String> collectionProducts = list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return productIds.stream().map(UUID::toString).anyMatch(collectionProducts::contains)
                || collectionProducts.contains(applicableCollectionId.toString());
    }

    private BigDecimal calculateDiscount(Promotion promotion,
                                         BigDecimal subtotal,
                                         List<PromotionEvaluationItemRequest> items) {
        Map<String, Object> conditions = promotion.getConditions() == null ? new LinkedHashMap<>() : promotion.getConditions();
        return switch (promotion.getType()) {
            case PERCENTAGE -> percentageDiscount(subtotal, numberValue(conditions.get("percent")));
            case FIXED -> fixedDiscount(subtotal, decimalValue(conditions.get("amount")));
            case CART_THRESHOLD_DISCOUNT -> thresholdDiscount(subtotal, conditions);
            case BUY_X_GET_Y -> buyXGetYDiscount(items, conditions);
            case PRODUCT_BUNDLE_DISCOUNT -> bundleDiscount(subtotal, items, conditions);
            case FREE_SHIPPING -> BigDecimal.ZERO;
        };
    }

    private BigDecimal percentageDiscount(BigDecimal subtotal, BigDecimal percent) {
        if (percent.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
    }

    private BigDecimal fixedDiscount(BigDecimal subtotal, BigDecimal amount) {
        if (amount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal thresholdDiscount(BigDecimal subtotal, Map<String, Object> conditions) {
        if (!meetsThreshold(conditions, subtotal)) {
            return BigDecimal.ZERO;
        }
        BigDecimal percent = numberValue(conditions.get("percent"));
        if (percent.signum() > 0) {
            return percentageDiscount(subtotal, percent);
        }
        return fixedDiscount(subtotal, decimalValue(conditions.get("amount")));
    }

    private boolean meetsThreshold(Map<String, Object> conditions, BigDecimal subtotal) {
        BigDecimal threshold = decimalValue(conditions == null ? null : conditions.get("threshold"));
        return threshold.signum() <= 0 || subtotal.compareTo(threshold) >= 0;
    }

    private BigDecimal buyXGetYDiscount(List<PromotionEvaluationItemRequest> items, Map<String, Object> conditions) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int buyQty = integerValue(conditions.get("buyQty"));
        int getQty = integerValue(conditions.get("getQty"));
        if (buyQty <= 0 || getQty <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discountPercent = numberValue(conditions.getOrDefault("discountPercent", 100));
        int totalQty = items.stream().mapToInt(PromotionEvaluationItemRequest::quantity).sum();
        if (totalQty < buyQty + getQty) {
            return BigDecimal.ZERO;
        }
        BigDecimal cheapestUnit = items.stream()
                .map(PromotionEvaluationItemRequest::unitPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        int eligibleFreeUnits = totalQty / (buyQty + getQty) * getQty;
        BigDecimal base = cheapestUnit.multiply(BigDecimal.valueOf(eligibleFreeUnits));
        return base.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal bundleDiscount(BigDecimal subtotal,
                                      List<PromotionEvaluationItemRequest> items,
                                      Map<String, Object> conditions) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<String> requestedProducts = items.stream()
                .map(PromotionEvaluationItemRequest::productId)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> bundleProducts = arrayStringSet(conditions.get("bundleProductIds"));
        if (!bundleProducts.isEmpty() && !requestedProducts.containsAll(bundleProducts)) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = decimalValue(conditions.get("bundleAmount"));
        if (amount.signum() > 0) {
            return fixedDiscount(subtotal, amount);
        }
        return percentageDiscount(subtotal, numberValue(conditions.get("percent")));
    }

    private Set<String> arrayStringSet(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return Set.of();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isWithinWindow(Instant now, Instant start, Instant end) {
        return (start == null || !now.isBefore(start)) && (end == null || !now.isAfter(end));
    }

    private BigDecimal numberValue(Object raw) {
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        if (raw instanceof String string && !string.isBlank()) {
            try {
                return new BigDecimal(string.trim()).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal decimalValue(Object raw) {
        return numberValue(raw);
    }

    private int integerValue(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String string && !string.isBlank()) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String resolveDisplayCode(Promotion promotion) {
        return trimToNull(promotion.getCode()) != null ? promotion.getCode().trim() : promotion.getName();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeToken(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
