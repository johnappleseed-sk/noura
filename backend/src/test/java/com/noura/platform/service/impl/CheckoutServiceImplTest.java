package com.noura.platform.service.impl;

import com.noura.platform.common.exception.UnauthorizedException;
import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.entity.Cart;
import com.noura.platform.domain.entity.CartItem;
import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductInventory;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.FulfillmentMethod;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.domain.enums.RefundStatus;
import com.noura.platform.dto.cart.CartTotalsDto;
import com.noura.platform.dto.order.CheckoutRequest;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.mapper.OrderMapper;
import com.noura.platform.repository.ApprovalRequestRepository;
import com.noura.platform.repository.B2BCompanyProfileRepository;
import com.noura.platform.repository.CartItemRepository;
import com.noura.platform.repository.CartRepository;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.OrderRepository;
import com.noura.platform.repository.OrderTimelineEventRepository;
import com.noura.platform.repository.ProductInventoryRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.AnalyticsEventService;
import com.noura.platform.service.PricingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductInventoryRepository inventoryRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderTimelineEventRepository orderTimelineEventRepository;
    @Mock private B2BCompanyProfileRepository companyProfileRepository;
    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private PricingService pricingService;
    @Mock private AnalyticsEventService analyticsEventService;

    private CheckoutServiceImpl checkoutService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "buyer@noura.test",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        AppProperties appProperties = new AppProperties();
        appProperties.getKafka().setEnabled(false);
        @SuppressWarnings("unchecked")
        org.springframework.kafka.core.KafkaTemplate<String, com.noura.platform.event.OrderCreatedEvent> kafkaTemplate = null;
        checkoutService = new CheckoutServiceImpl(
                userAccountRepository,
                cartRepository,
                cartItemRepository,
                inventoryRepository,
                orderRepository,
                orderItemRepository,
                orderTimelineEventRepository,
                companyProfileRepository,
                approvalRequestRepository,
                storeRepository,
                orderMapper,
                applicationEventPublisher,
                kafkaTemplate,
                appProperties,
                pricingService,
                analyticsEventService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkout_shouldReturnExistingOrderForIdempotencyKey() {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setEmail("buyer@noura.test");

        Order existing = new Order();
        existing.setId(UUID.randomUUID());
        existing.setUser(user);
        existing.setSubtotal(BigDecimal.TEN);
        existing.setDiscountAmount(BigDecimal.ZERO);
        existing.setShippingAmount(BigDecimal.ONE);
        existing.setTotalAmount(BigDecimal.valueOf(11));
        existing.setStatus(OrderStatus.PAID);
        existing.setRefundStatus(RefundStatus.NONE);
        existing.setFulfillmentMethod(FulfillmentMethod.DELIVERY);

        when(userAccountRepository.findByEmailIgnoreCase("buyer@noura.test")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserAndIdempotencyKey(user, "idem-checkout-1")).thenReturn(Optional.of(existing));
        when(orderItemRepository.findByOrderId(existing.getId())).thenReturn(List.of());
        when(orderMapper.toDto(existing)).thenReturn(toDto(existing));

        OrderDto result = checkoutService.checkout(new CheckoutRequest(
                FulfillmentMethod.DELIVERY,
                null,
                "Bangkok address",
                "PAY-123",
                null,
                false,
                "idem-checkout-1"
        ));

        assertEquals(existing.getId(), result.id());
        verify(cartRepository, never()).findByUser(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_shouldFailWhenStockDropsAfterCartReview() {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setEmail("buyer@noura.test");

        Store store = new Store();
        store.setId(UUID.randomUUID());

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Protein Bar");

        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);
        cart.setStore(store);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(5);
        item.setUnitPrice(BigDecimal.valueOf(2));

        Order pendingOrder = new Order();
        pendingOrder.setId(UUID.randomUUID());
        pendingOrder.setUser(user);
        pendingOrder.setStore(store);
        pendingOrder.setStatus(OrderStatus.PAID);
        pendingOrder.setRefundStatus(RefundStatus.NONE);

        ProductInventory lowStock = new ProductInventory();
        lowStock.setProduct(product);
        lowStock.setStore(store);
        lowStock.setStock(2);

        when(userAccountRepository.findByEmailIgnoreCase("buyer@noura.test")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserAndIdempotencyKey(user, "idem-checkout-2")).thenReturn(Optional.empty());
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item));
        when(pricingService.calculateTotals(List.of(item), store, null))
                .thenReturn(new CartTotalsDto(
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        BigDecimal.ONE,
                        BigDecimal.valueOf(11),
                        null,
                        List.of(),
                        false
                ));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);
        when(inventoryRepository.decrementStockIfAvailable(product.getId(), store.getId(), 5)).thenReturn(0);
        when(inventoryRepository.findByProductIdAndStoreId(product.getId(), store.getId())).thenReturn(Optional.of(lowStock));

        assertThrows(
                UnauthorizedException.class,
                () -> checkoutService.checkout(new CheckoutRequest(
                        FulfillmentMethod.DELIVERY,
                        store.getId(),
                        "Bangkok address",
                        "PAY-456",
                        null,
                        false,
                        "idem-checkout-2"
                ))
        );
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void checkout_shouldRejectExpiredCouponFromSharedPricingService() {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setEmail("buyer@noura.test");

        Store store = new Store();
        store.setId(UUID.randomUUID());

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Protein Bar");

        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);
        cart.setStore(store);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(BigDecimal.valueOf(50));

        when(userAccountRepository.findByEmailIgnoreCase("buyer@noura.test")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserAndIdempotencyKey(user, "idem-expired-coupon")).thenReturn(Optional.empty());
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item));
        when(pricingService.calculateTotals(List.of(item), store, "OLD10"))
                .thenThrow(new ForbiddenException("COUPON_EXPIRED", "Coupon has expired"));

        assertThrows(
                ForbiddenException.class,
                () -> checkoutService.checkout(new CheckoutRequest(
                        FulfillmentMethod.DELIVERY,
                        store.getId(),
                        "Bangkok address",
                        "PAY-999",
                        "OLD10",
                        false,
                        "idem-expired-coupon"
                ))
        );

        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
    }

    private OrderDto toDto(Order source) {
        return new OrderDto(
                source.getId(),
                source.getUser().getId(),
                source.getStore() == null ? null : source.getStore().getId(),
                source.getSubtotal(),
                source.getDiscountAmount(),
                source.getShippingAmount(),
                source.getTotalAmount(),
                source.getFulfillmentMethod(),
                source.getStatus(),
                source.getRefundStatus(),
                source.getCouponCode(),
                Instant.now(),
                List.of()
        );
    }
}
