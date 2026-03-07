package com.noura.platform.service;

import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.domain.enums.RefundStatus;
import com.noura.platform.dto.order.UpdateOrderStatusRequest;
import com.noura.platform.mapper.OrderMapper;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.OrderRepository;
import com.noura.platform.repository.OrderTimelineEventRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.NotificationService;
import com.noura.platform.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled("Temporarily disabled due Mockito/ByteBuddy instability on current Java runtime.")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderTimelineEventRepository orderTimelineEventRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserAccountRepository userAccountRepository;

    private OrderServiceImpl orderService;

    /**
     * Initializes test fixtures.
     */
    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                orderTimelineEventRepository,
                orderMapper,
                notificationService,
                userAccountRepository
        );
    }

    /**
     * Verifies that update status should reject invalid transition.
     */
    @Test
    void updateStatus_shouldRejectInvalidTransition() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.SHIPPED, RefundStatus.NONE);

        assertThrows(ForbiddenException.class, () -> orderService.updateStatus(orderId, request));
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderTimelineEventRepository, never()).save(any());
    }
}
