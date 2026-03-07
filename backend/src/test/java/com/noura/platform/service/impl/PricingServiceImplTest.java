package com.noura.platform.service.impl;

import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.CartItem;
import com.noura.platform.domain.entity.Coupon;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.dto.cart.CartTotalsDto;
import com.noura.platform.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Test
    void calculateTotals_shouldApplyCouponAndShippingFee() {
        PricingServiceImpl service = new PricingServiceImpl(couponRepository);
        Coupon save10 = coupon("SAVE10", 10, false, BigDecimal.ZERO);
        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("SAVE10")).thenReturn(Optional.of(save10));

        CartTotalsDto totals = service.calculateTotals(items(2, "50.00"), store("15.00", "200.00"), "save10");

        assertEquals(new BigDecimal("100.00"), totals.subtotal());
        assertEquals(new BigDecimal("10.00"), totals.discountAmount());
        assertEquals(new BigDecimal("15.00"), totals.shippingAmount());
        assertEquals(new BigDecimal("105.00"), totals.totalAmount());
        assertEquals("SAVE10", totals.couponCode());
    }

    @Test
    void calculateTotals_shouldApplyFreeShippingCoupon() {
        PricingServiceImpl service = new PricingServiceImpl(couponRepository);
        Coupon freeShip = coupon("FREESHIP5", 5, true, BigDecimal.ZERO);
        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("FREESHIP5")).thenReturn(Optional.of(freeShip));

        CartTotalsDto totals = service.calculateTotals(items(1, "100.00"), store("20.00", "500.00"), "FREESHIP5");

        assertEquals(new BigDecimal("5.00"), totals.discountAmount());
        assertEquals(BigDecimal.ZERO, totals.shippingAmount());
        assertEquals(new BigDecimal("95.00"), totals.totalAmount());
    }

    @Test
    void calculateTotals_shouldRejectInvalidCoupon() {
        PricingServiceImpl service = new PricingServiceImpl(couponRepository);
        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("BADCODE")).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.calculateTotals(items(1, "10.00"), store("5.00", "100.00"), "BADCODE")
        );
    }

    @Test
    void calculateTotals_shouldRejectWhenOrderBelowCouponMinimum() {
        PricingServiceImpl service = new PricingServiceImpl(couponRepository);
        Coupon premium = coupon("PREMIUM15", 15, false, new BigDecimal("100.00"));
        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("PREMIUM15")).thenReturn(Optional.of(premium));

        assertThrows(
                ForbiddenException.class,
                () -> service.calculateTotals(items(1, "50.00"), store("5.00", "100.00"), "PREMIUM15")
        );
    }

    @Test
    void calculateTotals_shouldRejectExpiredCoupon() {
        PricingServiceImpl service = new PricingServiceImpl(couponRepository);
        Coupon expired = coupon("OLD10", 10, false, BigDecimal.ZERO);
        expired.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("OLD10")).thenReturn(Optional.of(expired));

        assertThrows(
                ForbiddenException.class,
                () -> service.calculateTotals(items(1, "100.00"), store("5.00", "100.00"), "OLD10")
        );
    }

    @Test
    void calculateTotals_shouldRejectCouponBeforeValidFrom() {
        PricingServiceImpl service = new PricingServiceImpl(couponRepository);
        Coupon future = coupon("FUTURE10", 10, false, BigDecimal.ZERO);
        future.setValidFrom(Instant.now().plus(1, ChronoUnit.DAYS));
        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("FUTURE10")).thenReturn(Optional.of(future));

        assertThrows(
                ForbiddenException.class,
                () -> service.calculateTotals(items(1, "100.00"), store("5.00", "100.00"), "FUTURE10")
        );
    }

    @Test
    void calculateTotals_shouldRejectMultipleCouponInput() {
        PricingServiceImpl service = new PricingServiceImpl(couponRepository);

        assertThrows(
                ForbiddenException.class,
                () -> service.calculateTotals(items(1, "100.00"), store("5.00", "100.00"), "SAVE10,PREMIUM15")
        );
    }

    private List<CartItem> items(int quantity, String unitPrice) {
        CartItem item = new CartItem();
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        return List.of(item);
    }

    private Store store(String shippingFee, String freeShippingThreshold) {
        Store store = new Store();
        store.setShippingFee(new BigDecimal(shippingFee));
        store.setFreeShippingThreshold(new BigDecimal(freeShippingThreshold));
        return store;
    }

    private Coupon coupon(String code, int discountPercent, boolean freeShipping, BigDecimal minOrderValue) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountPercent(discountPercent);
        coupon.setFreeShipping(freeShipping);
        coupon.setMinOrderValue(minOrderValue);
        coupon.setActive(true);
        return coupon;
    }
}
