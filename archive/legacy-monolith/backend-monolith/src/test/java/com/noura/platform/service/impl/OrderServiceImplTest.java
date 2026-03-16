package com.noura.platform.service.impl;

import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.FulfillmentMethod;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.domain.enums.RefundStatus;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.UpdateOrderStatusRequest;
import com.noura.platform.mapper.OrderMapper;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.OrderRepository;
import com.noura.platform.repository.OrderTimelineEventRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private Order order;

    /**
     * Initializes test fixtures.
     */
    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CREATED);
        order.setRefundStatus(RefundStatus.NONE);
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        order.setUser(user);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    }

    /**
     * Updates status allows valid transition.
     */
    @Test
    void updateStatus_AllowsValidTransition() {
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toDto(any(Order.class))).thenAnswer(invocation -> {
            Order source = invocation.getArgument(0);
            return new OrderDto(
                    source.getId(),
                    null,
                    null,
                    BigDecimal.TEN,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.TEN,
                    FulfillmentMethod.PICKUP,
                    source.getStatus(),
                    source.getRefundStatus(),
                    null,
                    Instant.now(),
                    List.of()
            );
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.findByRole(RoleType.ADMIN)).thenReturn(List.of());

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.REVIEWED, RefundStatus.NONE);

        OrderDto updated = orderService.updateStatus(orderId, request);

        assertEquals(OrderStatus.REVIEWED, updated.status());
        verify(orderRepository, times(1)).save(order);
        verify(orderTimelineEventRepository, times(1)).save(any());
        verify(notificationService, times(1)).pushToUser(any(), any());
    }

    /**
     * Updates status rejects invalid transition.
     */
    @Test
    void updateStatus_RejectsInvalidTransition() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.SHIPPED, RefundStatus.NONE);

        assertThrows(ForbiddenException.class, () -> orderService.updateStatus(orderId, request));
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderTimelineEventRepository, never()).save(any());
    }
}
