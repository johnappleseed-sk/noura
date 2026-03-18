package com.noura.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.order.domain.entity.OrderRecord;
import com.noura.order.domain.entity.OrderStatusHistory;
import com.noura.order.domain.enums.OrderStatus;
import com.noura.order.domain.enums.RefundStatus;
import com.noura.order.dto.order.CreateOrderItemRequest;
import com.noura.order.dto.order.CreateOrderRequest;
import com.noura.order.dto.order.OrderResponse;
import com.noura.order.dto.order.QuickReorderResponse;
import com.noura.order.dto.order.UpdateOrderStatusRequest;
import com.noura.order.exception.OrderOperationException;
import com.noura.order.integration.client.CartServiceClient;
import com.noura.order.domain.entity.OrderItemRecord;
import com.noura.order.repository.OrderItemRecordRepository;
import com.noura.order.repository.OrderRecordRepository;
import com.noura.order.repository.OrderStatusHistoryRepository;
import com.noura.order.service.model.OrderRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRecordRepository orderRecordRepository;

    @Mock
    private OrderItemRecordRepository orderItemRecordRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private CartServiceClient cartServiceClient;

    private OrderServiceImpl orderService;

    /**
     * Initializes service under test before each test case.
     */
    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRecordRepository,
                orderItemRecordRepository,
                orderStatusHistoryRepository,
                new ObjectMapper(),
                cartServiceClient
        );
    }

    /**
     * Verifies idempotent create returns existing order and avoids duplicate writes.
     */
    @Test
    void shouldReturnExistingOrderWhenIdempotencyKeyMatches() {
        UUID orderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OrderRecord existing = new OrderRecord();
        existing.setId(orderId);
        existing.setOrderNumber("ORD-20260101-ABCDEF12");
        existing.setCustomerRef("customer-1");
        existing.setCurrencyCode("USD");
        existing.setSubtotal(new BigDecimal("10.0000"));
        existing.setDiscountAmount(new BigDecimal("0.0000"));
        existing.setShippingAmount(new BigDecimal("0.0000"));
        existing.setTaxAmount(new BigDecimal("0.0000"));
        existing.setTotalAmount(new BigDecimal("10.0000"));
        existing.setStatus(OrderStatus.PAID);
        existing.setRefundStatus(RefundStatus.NONE);
        existing.setPlacedAt(Instant.now());
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        when(orderRecordRepository.findByCustomerRefAndIdempotencyKey("customer-1", "idem-1"))
                .thenReturn(Optional.of(existing));
        when(orderItemRecordRepository.findByOrderIdOrderByLineNumberAsc(orderId)).thenReturn(List.of());

        OrderResponse response = orderService.createOrder(
                new OrderRequestContext("customer-1", Set.of(), false),
                new CreateOrderRequest(
                        null,
                        null,
                        null,
                        "USD",
                        "PAY-001",
                        null,
                        null,
                        null,
                        "Example shipping",
                        null,
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("10.00"),
                        true,
                        "idem-1",
                        List.of(new CreateOrderItemRequest(
                                UUID.randomUUID(),
                                null,
                                "SKU-1",
                                "Sample",
                                null,
                                1,
                                new BigDecimal("10.00"),
                                new BigDecimal("10.00"),
                                null
                        ))
                )
        );

        Assertions.assertEquals(orderId, response.id());
        verify(orderRecordRepository, never()).save(any(OrderRecord.class));
    }

    /**
     * Verifies invalid status transitions are rejected.
     */
    @Test
    void shouldRejectInvalidStatusTransition() {
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        OrderRecord existing = new OrderRecord();
        existing.setId(orderId);
        existing.setCustomerRef("customer-2");
        existing.setStatus(OrderStatus.SHIPPED);
        existing.setRefundStatus(RefundStatus.NONE);

        when(orderRecordRepository.findById(eq(orderId))).thenReturn(Optional.of(existing));

        OrderOperationException exception = Assertions.assertThrows(
                OrderOperationException.class,
                () -> orderService.updateOrderStatus(
                        new OrderRequestContext("admin", Set.of("ADMIN"), false),
                        orderId,
                        new UpdateOrderStatusRequest(OrderStatus.PAYMENT_PENDING, RefundStatus.NONE, null, null)
                )
        );

        Assertions.assertEquals("ORDER_STATUS_INVALID_TRANSITION", exception.getCode());
        verify(orderRecordRepository, never()).save(any(OrderRecord.class));
    }

    /**
     * Verifies quick reorder clears the current cart and rebuilds it from the stored order items.
     */
    @Test
    void shouldRebuildCartForQuickReorder() {
        UUID orderId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID storeId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID productId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID variantId = UUID.fromString("66666666-6666-6666-6666-666666666666");

        OrderRecord order = new OrderRecord();
        order.setId(orderId);
        order.setCustomerRef("customer-quick");
        order.setStoreId(storeId);
        order.setStatus(OrderStatus.DELIVERED);
        order.setRefundStatus(RefundStatus.NONE);

        OrderItemRecord item = new OrderItemRecord();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setLineNumber(1);
        item.setProductId(productId);
        item.setVariantId(variantId);
        item.setQuantity(2);

        when(orderRecordRepository.findById(eq(orderId))).thenReturn(Optional.of(order));
        when(orderItemRecordRepository.findByOrderIdOrderByLineNumberAsc(eq(orderId))).thenReturn(List.of(item));

        QuickReorderResponse response = orderService.quickReorder(
                new OrderRequestContext("customer-quick", Set.of(), false),
                orderId,
                "Bearer token",
                "corr-quick"
        );

        Assertions.assertEquals(orderId, response.orderId());
        Assertions.assertEquals(1, response.rebuiltItemCount());
        Assertions.assertTrue(response.replacedExistingCart());
        verify(cartServiceClient).clearCart("customer-quick", "Bearer token", "corr-quick");
        verify(cartServiceClient, times(1)).addItem(
                eq("customer-quick"),
                eq("Bearer token"),
                eq("corr-quick"),
                any(CartServiceClient.AddCartItemPayload.class)
        );
    }

    /**
     * Verifies successful order creation persists the initial payment-pending status history event.
     */
    @Test
    void shouldCreateOrderAndPersistInitialStatusHistory() {
        UUID storeId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID addressId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID productId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID createdOrderId = UUID.fromString("aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa");

        when(orderRecordRepository.findByCustomerRefAndIdempotencyKey("customer-create", "idem-create"))
                .thenReturn(Optional.empty());
        when(orderRecordRepository.save(any(OrderRecord.class))).thenAnswer(invocation -> {
            OrderRecord order = invocation.getArgument(0, OrderRecord.class);
            order.setId(createdOrderId);
            order.setCreatedAt(Instant.now());
            order.setUpdatedAt(Instant.now());
            return order;
        });
        when(orderItemRecordRepository.save(any(OrderItemRecord.class))).thenAnswer(invocation -> {
            OrderItemRecord item = invocation.getArgument(0, OrderItemRecord.class);
            item.setId(UUID.fromString("bbbbbbbb-1111-1111-1111-bbbbbbbbbbbb"));
            return item;
        });
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OrderStatusHistory.class));

        OrderResponse response = orderService.createOrder(
                new OrderRequestContext("customer-create", Set.of(), false),
                new CreateOrderRequest(
                        null,
                        storeId,
                        addressId,
                        "USD",
                        "PAY-123",
                        null,
                        null,
                        null,
                        "Line 1, Phnom Penh",
                        null,
                        new BigDecimal("25.0000"),
                        BigDecimal.ZERO.setScale(4),
                        BigDecimal.ZERO.setScale(4),
                        BigDecimal.ZERO.setScale(4),
                        new BigDecimal("25.0000"),
                        false,
                        "idem-create",
                        List.of(new CreateOrderItemRequest(
                                productId,
                                null,
                                "SKU-CREATE",
                                "Created Product",
                                null,
                                1,
                                new BigDecimal("25.0000"),
                                new BigDecimal("25.0000"),
                                null
                        ))
                )
        );

        ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);

        Assertions.assertEquals(createdOrderId, response.id());
        Assertions.assertEquals(OrderStatus.PAYMENT_PENDING, response.status());
        Assertions.assertEquals(1, response.items().size());
        verify(orderStatusHistoryRepository).save(historyCaptor.capture());
        Assertions.assertNull(historyCaptor.getValue().getFromStatus());
        Assertions.assertEquals(OrderStatus.PAYMENT_PENDING, historyCaptor.getValue().getToStatus());
        Assertions.assertEquals("ORDER_CREATED", historyCaptor.getValue().getReason());
    }

    /**
     * Verifies valid status updates persist a matching status-history entry.
     */
    @Test
    void shouldUpdateOrderStatusAndAppendHistory() {
        UUID orderId = UUID.fromString("cccccccc-1111-1111-1111-cccccccccccc");
        OrderRecord existing = new OrderRecord();
        existing.setId(orderId);
        existing.setCustomerRef("customer-update");
        existing.setStatus(OrderStatus.PAYMENT_PENDING);
        existing.setRefundStatus(RefundStatus.NONE);
        existing.setCurrencyCode("USD");
        existing.setSubtotal(new BigDecimal("25.0000"));
        existing.setDiscountAmount(BigDecimal.ZERO.setScale(4));
        existing.setShippingAmount(BigDecimal.ZERO.setScale(4));
        existing.setTaxAmount(BigDecimal.ZERO.setScale(4));
        existing.setTotalAmount(new BigDecimal("25.0000"));
        existing.setOrderNumber("ORD-STATUS-1");
        existing.setPlacedAt(Instant.now());
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        when(orderRecordRepository.findById(eq(orderId))).thenReturn(Optional.of(existing));
        when(orderRecordRepository.save(any(OrderRecord.class))).thenAnswer(invocation -> invocation.getArgument(0, OrderRecord.class));
        when(orderItemRecordRepository.findByOrderIdOrderByLineNumberAsc(orderId)).thenReturn(List.of());
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, OrderStatusHistory.class));

        OrderResponse response = orderService.updateOrderStatus(
                new OrderRequestContext("admin-1", Set.of("ADMIN"), false),
                orderId,
                new UpdateOrderStatusRequest(OrderStatus.PAID, RefundStatus.NONE, "PAYMENT_CAPTURED", "Captured successfully")
        );

        ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);

        Assertions.assertEquals(OrderStatus.PAID, response.status());
        verify(orderStatusHistoryRepository).save(historyCaptor.capture());
        Assertions.assertEquals(OrderStatus.PAYMENT_PENDING, historyCaptor.getValue().getFromStatus());
        Assertions.assertEquals(OrderStatus.PAID, historyCaptor.getValue().getToStatus());
        Assertions.assertEquals("PAYMENT_CAPTURED", historyCaptor.getValue().getReason());
        Assertions.assertEquals("Captured successfully", historyCaptor.getValue().getNote());
    }
}
