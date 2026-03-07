package com.noura.platform.service.impl;

import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.entity.OrderTimelineEvent;
import com.noura.platform.domain.enums.NotificationCategory;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.OrderItemDto;
import com.noura.platform.dto.order.OrderTimelineEventDto;
import com.noura.platform.dto.notification.SendNotificationRequest;
import com.noura.platform.dto.order.UpdateOrderStatusRequest;
import com.noura.platform.mapper.OrderMapper;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.OrderRepository;
import com.noura.platform.repository.OrderTimelineEventRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.NotificationService;
import com.noura.platform.service.OrderService;
import com.noura.platform.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> STATUS_FLOW = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.REVIEWED, OrderStatus.CANCELLED),
            OrderStatus.REVIEWED, Set.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_PENDING, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.PACKED, OrderStatus.REFUNDED),
            OrderStatus.PACKED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED, Set.of(OrderStatus.REFUNDED),
            OrderStatus.REFUNDED, Set.of()
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTimelineEventRepository orderTimelineEventRepository;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;
    private final UserAccountRepository userAccountRepository;

    /**
     * Executes admin orders.
     *
     * @param pageable The pagination configuration.
     * @return A paginated result set.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderDto> adminOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapWithItems);
    }

    /**
     * Retrieves by id.
     *
     * @param orderId The order id used to locate the target record.
     * @return The mapped DTO representation.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public OrderDto getById(UUID orderId) {
        return mapWithItems(orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found")));
    }

    /**
     * Retrieves order timeline.
     *
     * @param orderId The order id used to locate the target record.
     * @return A list of matching items.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderTimelineEventDto> orderTimeline(UUID orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new NotFoundException("ORDER_NOT_FOUND", "Order not found");
        }
        return orderTimelineEventRepository.findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(this::toTimelineDto)
                .toList();
    }

    /**
     * Updates status.
     *
     * @param orderId The order id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public OrderDto updateStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        OrderStatus previousStatus = order.getStatus();
        validateTransition(order.getStatus(), request.status());
        order.setStatus(request.status());
        order.setRefundStatus(request.refundStatus());
        Order saved = orderRepository.save(order);
        appendTimeline(saved, "Order status updated from " + previousStatus + " to " + saved.getStatus());
        notifyOrderStatusChange(saved, previousStatus);
        return mapWithItems(saved);
    }

    /**
     * Validates validate transition.
     *
     * @param current The current value.
     * @param next The next value.
     */
    private void validateTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return;
        }
        Set<OrderStatus> allowed = STATUS_FLOW.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new ForbiddenException("ORDER_STATUS_INVALID", "Invalid order status transition");
        }
    }

    /**
     * Transforms data for with items.
     *
     * @param order The order value.
     * @return The mapped DTO representation.
     */
    private OrderDto mapWithItems(Order order) {
        List<OrderItemDto> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(orderMapper::toItemDto)
                .toList();
        OrderDto dto = orderMapper.toDto(order);
        return new OrderDto(
                dto.id(),
                dto.userId(),
                dto.storeId(),
                dto.subtotal(),
                dto.discountAmount(),
                dto.shippingAmount(),
                dto.totalAmount(),
                dto.fulfillmentMethod(),
                dto.status(),
                dto.refundStatus(),
                dto.couponCode(),
                dto.createdAt(),
                items
        );
    }

    /**
     * Executes append timeline.
     *
     * @param order The order value.
     * @param note The note value.
     */
    private void appendTimeline(Order order, String note) {
        OrderTimelineEvent event = new OrderTimelineEvent();
        event.setOrder(order);
        event.setStatus(order.getStatus());
        event.setRefundStatus(order.getRefundStatus());
        String actor;
        try {
            actor = SecurityUtils.currentEmail();
        } catch (RuntimeException ex) {
            actor = "system";
        }
        event.setActor(actor);
        event.setNote(note);
        orderTimelineEventRepository.save(event);
    }

    /**
     * Executes notify order status change.
     *
     * @param order The order value.
     * @param previousStatus The previous status value.
     */
    private void notifyOrderStatusChange(Order order, OrderStatus previousStatus) {
        String orderRef = order.getId().toString();
        String title = "Order status updated";
        String body = "Order " + orderRef + " moved from " + previousStatus + " to " + order.getStatus() + ".";
        notificationService.pushToUser(
                order.getUser().getId(),
                new SendNotificationRequest(order.getUser().getId(), NotificationCategory.ORDER, title, body)
        );
        userAccountRepository.findByRole(RoleType.ADMIN).stream()
                .map(admin -> admin.getId())
                .filter(adminId -> !adminId.equals(order.getUser().getId()))
                .forEach(adminId -> notificationService.pushToUser(
                        adminId,
                        new SendNotificationRequest(adminId, NotificationCategory.ORDER, title, body)
                ));
    }

    /**
     * Maps source data to OrderTimelineEventDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    private OrderTimelineEventDto toTimelineDto(OrderTimelineEvent entity) {
        return new OrderTimelineEventDto(
                entity.getId(),
                entity.getOrder().getId(),
                entity.getStatus(),
                entity.getRefundStatus(),
                entity.getActor(),
                entity.getNote(),
                entity.getCreatedAt()
        );
    }
}
