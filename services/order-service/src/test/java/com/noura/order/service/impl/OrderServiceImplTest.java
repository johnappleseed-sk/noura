package com.noura.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.order.domain.entity.OrderRecord;
import com.noura.order.domain.enums.OrderStatus;
import com.noura.order.domain.enums.RefundStatus;
import com.noura.order.dto.order.CreateOrderItemRequest;
import com.noura.order.dto.order.CreateOrderRequest;
import com.noura.order.dto.order.OrderResponse;
import com.noura.order.dto.order.UpdateOrderStatusRequest;
import com.noura.order.exception.OrderOperationException;
import com.noura.order.repository.OrderItemRecordRepository;
import com.noura.order.repository.OrderRecordRepository;
import com.noura.order.repository.OrderStatusHistoryRepository;
import com.noura.order.service.model.OrderRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
                new ObjectMapper()
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
}

