package com.noura.checkout.controller;

import com.noura.checkout.controller.support.CheckoutRequestContextResolver;
import com.noura.checkout.dto.checkout.CheckoutPaymentSummaryResponse;
import com.noura.checkout.exception.ApiExceptionHandler;
import com.noura.checkout.integration.client.CartServiceClient;
import com.noura.checkout.integration.client.CustomerServiceClient;
import com.noura.checkout.integration.client.InventoryServiceClient;
import com.noura.checkout.integration.client.NotificationServiceClient;
import com.noura.checkout.integration.client.OrderServiceClient;
import com.noura.checkout.integration.client.PricingServiceClient;
import com.noura.checkout.service.CheckoutIdempotencyService;
import com.noura.checkout.service.PaymentGateway;
import com.noura.checkout.service.impl.CheckoutOrchestrationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-plus-service checkout tests that exercise HTTP contracts through the orchestration layer.
 */
@WebMvcTest(controllers = CheckoutController.class)
@Import({ApiExceptionHandler.class, CheckoutRequestContextResolver.class, CheckoutOrchestrationServiceImpl.class})
class CheckoutControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartServiceClient cartServiceClient;

    @MockBean
    private CustomerServiceClient customerServiceClient;

    @MockBean
    private PricingServiceClient pricingServiceClient;

    @MockBean
    private InventoryServiceClient inventoryServiceClient;

    @MockBean
    private OrderServiceClient orderServiceClient;

    @MockBean
    private NotificationServiceClient notificationServiceClient;

    @MockBean
    private CheckoutIdempotencyService checkoutIdempotencyService;

    @MockBean
    private PaymentGateway paymentGateway;

    /**
     * Verifies checkout validation returns an invalid response when one line fails stock checks.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void validateReturnsInvalidPreviewWhenStockIsInsufficient() throws Exception {
        UUID cartId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID storeId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID addressId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID itemId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID productId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        when(cartServiceClient.getActiveCart("customer-validate", "Bearer token", "corr-validate"))
                .thenReturn(cart(cartId, storeId, addressId, itemId, productId, 2, "12.5000", "25.0000", "VALID", null));
        when(customerServiceClient.getAddress("customer-validate", addressId, "Bearer token", "corr-validate"))
                .thenReturn(address(addressId));
        when(pricingServiceClient.resolvePrice(productId, storeId, "corr-validate"))
                .thenReturn(Optional.of(new PricingServiceClient.PricePayload(productId, "USD", new BigDecimal("12.5000"))));
        when(inventoryServiceClient.resolveAvailable(productId, storeId, "corr-validate"))
                .thenReturn(new BigDecimal("1.0000"));

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .header("X-Auth-Subject", "customer-validate")
                        .header("Authorization", "Bearer token")
                        .header("X-Correlation-ID", "corr-validate")
                        .contentType("application/json")
                        .content("""
                                {
                                  "storeId": "22222222-2222-2222-2222-222222222222",
                                  "addressId": "33333333-3333-3333-3333-333333333333"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Checkout validation"))
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.issues[0].code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.data.preview.lines[0].availableQuantity").value(1))
                .andExpect(jsonPath("$.data.preview.lines[0].valid").value(false));

        verify(orderServiceClient, never()).createOrder(any(), any(), any(), any());
    }

    /**
     * Verifies place-order HTTP flow returns created order and payment summaries after synchronous orchestration.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void placeOrderReturnsCreatedResponseForHappyPath() throws Exception {
        UUID cartId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID storeId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID addressId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID itemId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID productId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID movementId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UUID orderId = UUID.fromString("12121212-1212-1212-1212-121212121212");
        UUID paymentId = UUID.fromString("34343434-3434-3434-3434-343434343434");
        UUID customerId = UUID.fromString("56565656-5656-5656-5656-565656565656");

        when(cartServiceClient.getActiveCart("customer-place", "Bearer token", "corr-place"))
                .thenReturn(cart(cartId, storeId, addressId, itemId, productId, 1, "25.0000", "25.0000", "VALID", null));
        when(customerServiceClient.getAddress("customer-place", addressId, "Bearer token", "corr-place"))
                .thenReturn(address(addressId));
        when(pricingServiceClient.resolvePrice(productId, storeId, "corr-place"))
                .thenReturn(Optional.of(new PricingServiceClient.PricePayload(productId, "USD", new BigDecimal("25.0000"))));
        when(inventoryServiceClient.resolveAvailable(productId, storeId, "corr-place"))
                .thenReturn(new BigDecimal("8.0000"));
        when(inventoryServiceClient.reserve(
                eq(productId),
                eq(storeId),
                eq(new BigDecimal("1.0000")),
                eq("customer-place"),
                any(),
                eq("corr-place")
        )).thenReturn(new InventoryServiceClient.ReservationResult(
                movementId,
                productId,
                storeId,
                new BigDecimal("1.0000")
        ));
        when(orderServiceClient.createOrder(eq("customer-place"), eq("Bearer token"), eq("corr-place"), any()))
                .thenReturn(new OrderServiceClient.OrderPayload(
                        orderId,
                        "ORD-2001",
                        "PAYMENT_PENDING",
                        new BigDecimal("25.0000"),
                        "USD",
                        Instant.parse("2026-03-18T09:00:00Z"),
                        Instant.parse("2026-03-18T09:00:00Z")
                ));
        when(paymentGateway.createAndConfirmPayment(any(), any(), eq(orderId), eq("USD"), eq(new BigDecimal("25.0000")), eq(null)))
                .thenReturn(new CheckoutPaymentSummaryResponse(
                        paymentId,
                        "PAY-2001",
                        "mock",
                        "CARD",
                        "CAPTURED",
                        new BigDecimal("25.0000"),
                        "USD",
                        Instant.parse("2026-03-18T09:00:05Z")
                ));
        when(orderServiceClient.updateOrderStatusInternal(eq(orderId), eq("corr-place"), any()))
                .thenReturn(new OrderServiceClient.OrderPayload(
                        orderId,
                        "ORD-2001",
                        "PAID",
                        new BigDecimal("25.0000"),
                        "USD",
                        Instant.parse("2026-03-18T09:00:00Z"),
                        Instant.parse("2026-03-18T09:00:06Z")
                ));
        when(customerServiceClient.lookupByExternalSubject("customer-place", "corr-place"))
                .thenReturn(new CustomerServiceClient.CustomerLookupPayload(
                        customerId,
                        "customer-place",
                        "Noura User",
                        "customer@noura.test",
                        "012345678",
                        true,
                        addressId,
                        addressId
                ));

        mockMvc.perform(post("/api/v1/checkout/place-order")
                        .header("X-Auth-Subject", "customer-place")
                        .header("Authorization", "Bearer token")
                        .header("X-Correlation-ID", "corr-place")
                        .contentType("application/json")
                        .content("""
                                {
                                  "storeId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                                  "addressId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                  "paymentMethod": "CREDIT_CARD",
                                  "paymentProvider": "mock",
                                  "paymentAutoCapture": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order placed"))
                .andExpect(jsonPath("$.data.order.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.order.status").value("PAID"))
                .andExpect(jsonPath("$.data.payment.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.payment.status").value("CAPTURED"))
                .andExpect(jsonPath("$.data.reservedStock[0].movementId").value(movementId.toString()))
                .andExpect(jsonPath("$.data.replayed").value(false));

        verify(cartServiceClient).clearCart("customer-place", "Bearer token", "corr-place");
        verify(notificationServiceClient).sendOrderPlacedNotification(customerId, "ORD-2001", "corr-place");
    }

    /**
     * Creates a minimal cart payload fixture.
     */
    private CartServiceClient.CartPayload cart(
            UUID cartId,
            UUID storeId,
            UUID addressId,
            UUID itemId,
            UUID productId,
            int quantity,
            String unitPrice,
            String lineTotal,
            String validationStatus,
            String validationMessage
    ) {
        return new CartServiceClient.CartPayload(
                cartId,
                storeId,
                addressId,
                "USD",
                List.of(new CartServiceClient.CartItemPayload(
                        itemId,
                        productId,
                        null,
                        storeId,
                        "Demo Product",
                        "SKU-2001",
                        quantity,
                        new BigDecimal(unitPrice),
                        new BigDecimal(lineTotal),
                        new BigDecimal("8.0000"),
                        validationStatus,
                        validationMessage
                )),
                new CartServiceClient.CartTotalsPayload(
                        new BigDecimal(lineTotal),
                        BigDecimal.ZERO.setScale(4),
                        BigDecimal.ZERO.setScale(4),
                        new BigDecimal(lineTotal),
                        null
                )
        );
    }

    /**
     * Creates a valid shipping address fixture used by controller/service tests.
     */
    private CustomerServiceClient.AddressPayload address(UUID addressId) {
        return new CustomerServiceClient.AddressPayload(
                addressId,
                "Noura User",
                "012345678",
                "Line 1",
                null,
                "District",
                "Phnom Penh",
                "Phnom Penh",
                "12000",
                "KH",
                "Line 1, Phnom Penh",
                "VALID"
        );
    }
}
