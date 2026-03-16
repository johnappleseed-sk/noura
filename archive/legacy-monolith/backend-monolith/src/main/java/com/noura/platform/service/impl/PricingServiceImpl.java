package com.noura.platform.service.impl;

import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.CartItem;
import com.noura.platform.domain.entity.Coupon;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.dto.cart.CartTotalsDto;
import com.noura.platform.dto.pricing.PromotionEvaluationItemRequest;
import com.noura.platform.dto.pricing.PromotionEvaluationDto;
import com.noura.platform.repository.CouponRepository;
import com.noura.platform.service.PricingService;
import com.noura.platform.service.PromotionRuleEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final CouponRepository couponRepository;
    private final PromotionRuleEngineService promotionRuleEngineService;

    /**
     * Calculates totals.
     *
     * @param cartItems The cart items value.
     * @param store The store value.
     * @param couponCode The coupon code value.
     * @return The mapped DTO representation.
     */
    @Override
    public CartTotalsDto calculateTotals(List<CartItem> cartItems, Store store, String couponCode) {
        BigDecimal subtotal = cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Coupon coupon = resolveCoupon(couponCode, subtotal);
        int discountPercent = coupon == null ? 0 : coupon.getDiscountPercent();
        BigDecimal couponDiscount = subtotal
                .multiply(BigDecimal.valueOf(discountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        PromotionEvaluationDto promotionEvaluation = promotionRuleEngineService.evaluate(
                subtotal,
                couponCode,
                null,
                cartItems.stream()
                        .map(item -> new PromotionEvaluationItemRequest(
                                item.getProduct().getId(),
                                item.getProduct().getCategory() == null ? null : item.getProduct().getCategory().getId(),
                                item.getQuantity(),
                                item.getUnitPrice()
                        ))
                        .toList()
        );
        BigDecimal discount = couponDiscount.add(promotionEvaluation.discountAmount()).min(subtotal);

        BigDecimal shipping = BigDecimal.ZERO;
        if (store != null) {
            boolean freeShipping = subtotal.compareTo(store.getFreeShippingThreshold()) >= 0
                    || (coupon != null && coupon.isFreeShipping())
                    || promotionEvaluation.freeShipping();
            if (!freeShipping) {
                shipping = store.getShippingFee();
            }
        }

        String normalizedCoupon = coupon == null ? null : coupon.getCode().toUpperCase();
        return new CartTotalsDto(
                subtotal,
                discount,
                shipping,
                subtotal.subtract(discount).add(shipping),
                normalizedCoupon,
                promotionEvaluation.appliedPromotionCodes(),
                promotionEvaluation.freeShipping()
        );
    }

    private Coupon resolveCoupon(String couponCode, BigDecimal subtotal) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        if (containsMultipleCouponCodes(couponCode)) {
            throw new ForbiddenException(
                    "COUPON_MULTI_NOT_SUPPORTED",
                    "Only one coupon code can be applied per order"
            );
        }
        String normalized = couponCode.trim().toUpperCase();
        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndActiveTrue(normalized)
                .orElseThrow(() -> new NotFoundException("COUPON_INVALID", "Unsupported coupon"));
        Instant now = Instant.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            throw new ForbiddenException("COUPON_NOT_ACTIVE", "Coupon is not active yet");
        }
        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
            throw new ForbiddenException("COUPON_EXPIRED", "Coupon has expired");
        }
        if (subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new ForbiddenException(
                    "COUPON_MIN_ORDER",
                    "Coupon requires minimum order value of " + coupon.getMinOrderValue()
            );
        }
        return coupon;
    }

    private boolean containsMultipleCouponCodes(String couponCode) {
        String normalized = couponCode.trim();
        return normalized.contains(",") || normalized.contains(";");
    }
}
