package com.noura.checkout.service.impl;

import com.noura.checkout.dto.checkout.CheckoutOrderSummaryResponse;
import com.noura.checkout.dto.checkout.CheckoutPaymentSummaryResponse;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderRequest;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderResponse;
import com.noura.checkout.exception.CheckoutOperationException;
import com.noura.checkout.integration.client.CartServiceClient;
import com.noura.checkout.integration.client.CustomerServiceClient;
import com.noura.checkout.integration.client.InventoryServiceClient;
import com.noura.checkout.integration.client.NotificationServiceClient;
import com.noura.checkout.integration.client.OrderServiceClient;
import com.noura.checkout.integration.client.PricingServiceClient;
import com.noura.checkout.service.CheckoutIdempotencyService;
import com.noura.checkout.service.PaymentGateway;
import com.noura.checkout.service.model.CheckoutRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CheckoutOrchestrationServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutOrchestrationServiceImplTest {

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private PricingServiceClient pricingServiceClient;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @Mock
    private CheckoutIdempotencyService checkoutIdempotencyService;

    @Mock
    private PaymentGateway paymentGateway;

    private CheckoutOrchestrationServiceImpl checkoutService;

    /**
     * Initializes service under test with mocked dependencies.
     */
    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutOrchestrationServiceImpl(
                cartServiceClient,
                customerServiceClient,
                pricingServiceClient,
                inventoryServiceClient,
                orderServiceClient,
                notificationServiceClient,
                checkoutIdempotencyService,
                paymentGateway
        );
    }

    /**
     * Verifies reservation rollback is executed when order creation fails.
     */
    @Test
    void shouldReleaseReservationsWhenOrderCreationFails() {
        UUID cartId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();

        CheckoutRequestContext context = new CheckoutRequestContext(
                "customer-1",
                "Bearer token",
                "corr-1",
                Set.of("CUSTOMER")
        );

        CartServiceClient.CartItemPayload item = new CartServiceClient.CartItemPayload(
                itemId,
                productId,
                null,
                storeId,
                "Demo Product",
                "SKU-001",
                2,
                new BigDecimal("10.0000"),
                new BigDecimal("20.0000"),
                new BigDecimal("5.0000"),
                "VALID",
                null
        );
        CartServiceClient.CartPayload cart = new CartServiceClient.CartPayload(
                cartId,
                storeId,
                addressId,
                "USD",
                List.of(item),
                new CartServiceClient.CartTotalsPayload(
                        new BigDecimal("20.0000"),
                        BigDecimal.ZERO.setScale(4),
                        BigDecimal.ZERO.setScale(4),
                        new BigDecimal("20.0000"),
                        null
                )
        );
        CustomerServiceClient.AddressPayload address = new CustomerServiceClient.AddressPayload(
                addressId,
                "Noura User",
                "012345678",
                "Line 1",
                null,
                "District",
                "City",
                "State",
                "12000",
                "KH",
                "Line 1, City",
                "VALID"
        );

        when(cartServiceClient.getActiveCart(eq("customer-1"), eq("Bearer token"), eq("corr-1"))).thenReturn(cart);
        when(customerServiceClient.getAddress(eq("customer-1"), eq(addressId), eq("Bearer token"), eq("corr-1")))
                .thenReturn(address);
        when(pricingServiceClient.resolvePrice(eq(productId), eq(storeId), eq("corr-1")))
                .thenReturn(Optional.of(new PricingServiceClient.PricePayload(productId, "USD", new BigDecimal("10.0000"))));
        when(inventoryServiceClient.resolveAvailable(eq(productId), eq(storeId), eq("corr-1")))
                .thenReturn(new BigDecimal("5.0000"));
        when(inventoryServiceClient.reserve(
                eq(productId),
                eq(storeId),
                eq(new BigDecimal("2.0000")),
                eq("customer-1"),
                any(),
                eq("corr-1")
        )).thenReturn(new InventoryServiceClient.ReservationResult(
                movementId,
                productId,
                storeId,
                new BigDecimal("2.0000")
        ));
        when(orderServiceClient.createOrder(eq("customer-1"), eq("Bearer token"), eq("corr-1"), any()))
                .thenThrow(new CheckoutOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "ORDER_SERVICE_ERROR",
                        "order service unavailable"
                ));

        CheckoutOperationException exception = Assertions.assertThrows(
                CheckoutOperationException.class,
                () -> checkoutService.placeOrder(
                        context,
                        new CheckoutPlaceOrderRequest(storeId, addressId, null, null, null, null, null, null),
                        null
                )
        );

        Assertions.assertEquals("ORDER_SERVICE_ERROR", exception.getCode());
        verify(inventoryServiceClient).release(
                eq(productId),
                eq(storeId),
                eq(new BigDecimal("2.0000")),
                eq("customer-1"),
                any(),
                eq("corr-1")
        );
        verify(cartServiceClient, never()).clearCart(any(), any(), any());
    }

    /**
     * Verifies successful checkout creates payment, finalizes order, clears the cart, and dispatches notification.
     */
    @Test
    void shouldFinalizePaidOrderAfterSynchronousPayment() {
        UUID cartId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();

        CheckoutRequestContext context = new CheckoutRequestContext(
                "customer-3",
                "Bearer token",
                "corr-3",
                Set.of("CUSTOMER")
        );

        CartServiceClient.CartItemPayload item = new CartServiceClient.CartItemPayload(
                itemId,
                productId,
                null,
                storeId,
                "Demo Product",
                "SKU-002",
                1,
                new BigDecimal("25.0000"),
                new BigDecimal("25.0000"),
                new BigDecimal("8.0000"),
                "VALID",
                null
        );
        CartServiceClient.CartPayload cart = new CartServiceClient.CartPayload(
                cartId,
                storeId,
                addressId,
                "USD",
                List.of(item),
                new CartServiceClient.CartTotalsPayload(
                        new BigDecimal("25.0000"),
                        BigDecimal.ZERO.setScale(4),
                        BigDecimal.ZERO.setScale(4),
                        new BigDecimal("25.0000"),
                        null
                )
        );
        CustomerServiceClient.AddressPayload address = new CustomerServiceClient.AddressPayload(
                addressId,
                "Noura User",
                "012345678",
                "Line 1",
                null,
                "District",
                "City",
                "State",
                "12000",
                "KH",
                "Line 1, City",
                "VALID"
        );
        OrderServiceClient.OrderPayload createdOrder = new OrderServiceClient.OrderPayload(
                orderId,
                "ORD-1001",
                "PAYMENT_PENDING",
                new BigDecimal("25.0000"),
                "USD",
                Instant.now(),
                Instant.now()
        );
        OrderServiceClient.OrderPayload finalizedOrder = new OrderServiceClient.OrderPayload(
                orderId,
                "ORD-1001",
                "PAID",
                new BigDecimal("25.0000"),
                "USD",
                Instant.now(),
                Instant.now()
        );
        CheckoutPaymentSummaryResponse payment = new CheckoutPaymentSummaryResponse(
                paymentId,
                "PAY-1001",
                "mock",
                "CARD",
                "CAPTURED",
                new BigDecimal("25.0000"),
                "USD",
                Instant.now()
        );

        when(cartServiceClient.getActiveCart(eq("customer-3"), eq("Bearer token"), eq("corr-3"))).thenReturn(cart);
        when(customerServiceClient.getAddress(eq("customer-3"), eq(addressId), eq("Bearer token"), eq("corr-3")))
                .thenReturn(address);
        when(pricingServiceClient.resolvePrice(eq(productId), eq(storeId), eq("corr-3")))
                .thenReturn(Optional.of(new PricingServiceClient.PricePayload(productId, "USD", new BigDecimal("25.0000"))));
        when(inventoryServiceClient.resolveAvailable(eq(productId), eq(storeId), eq("corr-3")))
                .thenReturn(new BigDecimal("8.0000"));
        when(inventoryServiceClient.reserve(
                eq(productId),
                eq(storeId),
                eq(new BigDecimal("1.0000")),
                eq("customer-3"),
                any(),
                eq("corr-3")
        )).thenReturn(new InventoryServiceClient.ReservationResult(
                movementId,
                productId,
                storeId,
                new BigDecimal("1.0000")
        ));
        when(orderServiceClient.createOrder(eq("customer-3"), eq("Bearer token"), eq("corr-3"), any()))
                .thenReturn(createdOrder);
        when(paymentGateway.createAndConfirmPayment(eq(context), any(), eq(orderId), eq("USD"), eq(new BigDecimal("25.0000")), eq("idem-3")))
                .thenReturn(payment);
        when(orderServiceClient.updateOrderStatusInternal(eq(orderId), eq("corr-3"), any()))
                .thenReturn(finalizedOrder);
        when(customerServiceClient.lookupByExternalSubject(eq("customer-3"), eq("corr-3")))
                .thenReturn(new CustomerServiceClient.CustomerLookupPayload(
                        customerId,
                        "customer-3",
                        "Noura User",
                        "customer@noura.test",
                        "012345678",
                        true,
                        addressId,
                        addressId
                ));

        CheckoutPlaceOrderResponse response = checkoutService.placeOrder(
                context,
                new CheckoutPlaceOrderRequest(
                        storeId,
                        addressId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "idem-3"
                ),
                null
        );

        Assertions.assertEquals("PAID", response.order().status());
        Assertions.assertEquals("CAPTURED", response.payment().status());
        verify(paymentGateway).createAndConfirmPayment(eq(context), any(), eq(orderId), eq("USD"), eq(new BigDecimal("25.0000")), eq("idem-3"));
        verify(orderServiceClient).updateOrderStatusInternal(eq(orderId), eq("corr-3"), any());
        verify(cartServiceClient).clearCart("customer-3", "Bearer token", "corr-3");
        verify(notificationServiceClient).sendOrderPlacedNotification(customerId, "ORD-1001", "corr-3");
    }

    /**
     * Verifies successful idempotency replay bypasses orchestration dependencies.
     */
    @Test
    void shouldReturnReplayWhenIdempotencyAlreadySucceeded() {
        CheckoutRequestContext context = new CheckoutRequestContext(
                "customer-2",
                "Bearer token",
                "corr-2",
                Set.of("CUSTOMER")
        );

        CheckoutPlaceOrderResponse replay = new CheckoutPlaceOrderResponse(
                new CheckoutOrderSummaryResponse(
                        UUID.randomUUID(),
                        "ORD-REPLAY",
                        "PAID",
                        new BigDecimal("50.0000"),
                        "USD",
                        Instant.now()
                ),
                new CheckoutPaymentSummaryResponse(
                        UUID.randomUUID(),
                        "PAY-REPLAY",
                        "mock",
                        "CARD",
                        "CAPTURED",
                        new BigDecimal("50.0000"),
                        "USD",
                        Instant.now()
                ),
                List.of(),
                "idem-1",
                true,
                Instant.now(),
                "Order placed successfully"
        );
        when(checkoutIdempotencyService.tryReplay("customer-2", "idem-1")).thenReturn(Optional.of(replay));

        CheckoutPlaceOrderResponse response = checkoutService.placeOrder(
                context,
                new CheckoutPlaceOrderRequest(null, null, null, null, null, null, null, "idem-1"),
                null
        );

        Assertions.assertTrue(response.replayed());
        verify(checkoutIdempotencyService, never()).beginProcessing(any(), any(), any(), any());
        verifyNoInteractions(cartServiceClient, customerServiceClient, pricingServiceClient, inventoryServiceClient, orderServiceClient);
    }
}
