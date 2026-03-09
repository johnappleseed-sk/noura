package com.noura.platform.service.impl;

import com.noura.platform.commerce.orders.application.StorefrontOrderService;
import com.noura.platform.dto.cart.CartDto;
import com.noura.platform.dto.cart.CartTotalsDto;
import com.noura.platform.dto.order.CheckoutStepPreviewDto;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.UpdateOrderStatusRequest;
import com.noura.platform.dto.storefront.StorefrontOrderResult;
import com.noura.platform.dto.storefront.StorefrontOrderSummaryDto;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.service.CartService;
import com.noura.platform.service.CheckoutService;
import com.noura.platform.service.OrderService;
import com.noura.platform.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedOrderServiceImplTest {

    @Mock
    private OrderService platformOrderService;

    @Mock
    private CheckoutService platformCheckoutService;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private CartService cartService;

    private UnifiedOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UnifiedOrderServiceImpl(
                platformOrderService,
                platformCheckoutService,
                userAccountService,
                cartService,
                orderProvider(null),
                cartProvider(null)
        );
    }

    @Test
    void adminOrdersDelegatesToPlatformOrderService() {
        var pageable = PageRequest.of(0, 20);
        Page<OrderDto> expected = Page.empty(pageable);
        when(platformOrderService.adminOrders(pageable)).thenReturn(expected);

        Page<OrderDto> actual = service.adminOrders(pageable);

        assertThat(actual).isSameAs(expected);
        verify(platformOrderService).adminOrders(pageable);
    }

    @Test
    void getPlatformOrderByIdDelegatesToPlatformOrderService() {
        UUID orderId = UUID.randomUUID();
        OrderDto expected = new OrderDto(orderId, UUID.randomUUID(), null, BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, null, OrderStatus.CREATED,
                null, null, null, List.of());
        when(platformOrderService.getById(orderId)).thenReturn(expected);

        OrderDto actual = service.getPlatformOrderById(orderId);

        assertThat(actual).isSameAs(expected);
        verify(platformOrderService).getById(orderId);
    }

    @Test
    void updateOrderStatusDelegatesToPlatformOrderService() {
        UUID orderId = UUID.randomUUID();
        var request = new UpdateOrderStatusRequest(OrderStatus.REVIEWED, null);
        OrderDto expected = new OrderDto(orderId, UUID.randomUUID(), null, BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, null, OrderStatus.REVIEWED,
                null, null, null, List.of());
        when(platformOrderService.updateStatus(orderId, request)).thenReturn(expected);

        OrderDto actual = service.updateOrderStatus(orderId, request);

        assertThat(actual).isSameAs(expected);
        verify(platformOrderService).updateStatus(orderId, request);
    }

    @Test
    void reviewCheckoutStepBuildsPreviewFromCartService() {
        CartDto cart = new CartDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(),
                new CartTotalsDto(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, null)
        );
        when(cartService.getMyCart()).thenReturn(cart);

        CheckoutStepPreviewDto actual = service.reviewCheckoutStep();

        assertThat(actual.step()).isEqualTo("review");
        assertThat(actual.nextStep()).isEqualTo("shipping");
        assertThat(actual.cart()).isSameAs(cart);
        verify(cartService).getMyCart();
    }

    @Test
    void myOrderHistoryDelegatesToUserAccountService() {
        List<OrderDto> expected = List.of();
        when(userAccountService.myOrderHistory()).thenReturn(expected);

        List<OrderDto> actual = service.myOrderHistory();

        assertThat(actual).isSameAs(expected);
        verify(userAccountService).myOrderHistory();
    }

    @Test
    void listStorefrontOrdersDelegatesWhenLegacyServiceIsAvailable() {
        List<StorefrontOrderSummaryDto> expected = List.of(
                new StorefrontOrderSummaryDto(
                        42L, "ORD-20260307-1-1001", "PENDING_PAYMENT",
                        LocalDateTime.now(), "USD", BigDecimal.TEN, 1
                )
        );
        var storefrontOrderService = storefrontOrderServiceStub(expected);
        service = new UnifiedOrderServiceImpl(
                platformOrderService, platformCheckoutService, userAccountService,
                cartService, orderProvider(storefrontOrderService), cartProvider(null)
        );

        List<StorefrontOrderSummaryDto> actual = service.listStorefrontOrders(1L);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void listStorefrontOrdersFailsFastWhenLegacyServiceIsInactive() {
        assertThatThrownBy(() -> service.listStorefrontOrders(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Legacy storefront order service is not active");
    }

    @Test
    void cancelStorefrontOrderDelegatesWhenLegacyServiceIsAvailable() {
        var expected = new StorefrontOrderResult(
                42L, "ORD-20260307-1-1001", "CANCELLED", "USD",
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.TEN, LocalDateTime.now(), null, null, List.of()
        );
        var storefrontOrderService = storefrontOrderServiceCancelStub(expected);
        service = new UnifiedOrderServiceImpl(
                platformOrderService, platformCheckoutService, userAccountService,
                cartService, orderProvider(storefrontOrderService), cartProvider(null)
        );

        var actual = service.cancelStorefrontOrder(1L, 42L);

        assertThat(actual).isEqualTo(expected);
    }

    // --- helper stubs ---

    private StorefrontOrderService storefrontOrderServiceStub(
            List<StorefrontOrderSummaryDto> orders) {
        return new StorefrontOrderService(null, null, null, null, null, null, null, null) {
            @Override
            public List<StorefrontOrderSummaryDto> listOrders(Long customerId) {
                return orders;
            }
        };
    }

    private StorefrontOrderService storefrontOrderServiceCancelStub(
            StorefrontOrderResult result) {
        return new StorefrontOrderService(null, null, null, null, null, null, null, null) {
            @Override
            public StorefrontOrderResult cancel(Long customerId, Long orderId) {
                return result;
            }
        };
    }

    private ObjectProvider<StorefrontOrderService> orderProvider(StorefrontOrderService svc) {
        return new ObjectProvider<>() {
            @Override
            public StorefrontOrderService getObject(Object... args) { return getIfAvailable(); }
            @Override
            public StorefrontOrderService getIfAvailable() { return svc; }
            @Override
            public StorefrontOrderService getIfUnique() { return svc; }
            @Override
            public StorefrontOrderService getObject() {
                if (svc == null) throw new IllegalStateException("Legacy storefront order service is not active in the current runtime profile.");
                return svc;
            }
            @Override
            public Iterator<StorefrontOrderService> iterator() {
                return svc == null ? List.<StorefrontOrderService>of().iterator() : List.of(svc).iterator();
            }
            @Override
            public Stream<StorefrontOrderService> stream() {
                return svc == null ? Stream.empty() : Stream.of(svc);
            }
        };
    }

    private ObjectProvider<com.noura.platform.commerce.cart.application.StorefrontCartService> cartProvider(
            com.noura.platform.commerce.cart.application.StorefrontCartService svc) {
        return new ObjectProvider<>() {
            @Override
            public com.noura.platform.commerce.cart.application.StorefrontCartService getObject(Object... args) { return getIfAvailable(); }
            @Override
            public com.noura.platform.commerce.cart.application.StorefrontCartService getIfAvailable() { return svc; }
            @Override
            public com.noura.platform.commerce.cart.application.StorefrontCartService getIfUnique() { return svc; }
            @Override
            public com.noura.platform.commerce.cart.application.StorefrontCartService getObject() {
                if (svc == null) throw new IllegalStateException("Storefront cart service is not active in the current runtime profile.");
                return svc;
            }
            @Override
            public Iterator<com.noura.platform.commerce.cart.application.StorefrontCartService> iterator() {
                return svc == null ? List.<com.noura.platform.commerce.cart.application.StorefrontCartService>of().iterator() : List.of(svc).iterator();
            }
            @Override
            public Stream<com.noura.platform.commerce.cart.application.StorefrontCartService> stream() {
                return svc == null ? Stream.empty() : Stream.of(svc);
            }
        };
    }
}
