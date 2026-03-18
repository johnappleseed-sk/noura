package com.noura.order.service;

import com.noura.order.domain.enums.OrderStatus;
import com.noura.order.domain.enums.RefundStatus;
import com.noura.order.dto.order.CreateOrderRequest;
import com.noura.order.dto.order.OrderResponse;
import com.noura.order.dto.order.OrderStatusEventResponse;
import com.noura.order.dto.order.UpdateOrderStatusRequest;
import com.noura.order.service.model.OrderRequestContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Order command/query service contract.
 */
public interface OrderService {

    /**
     * Creates an order from checkout payload.
     *
     * @param context request actor context
     * @param request order creation payload
     * @return created or idempotency-resolved order
     */
    OrderResponse createOrder(OrderRequestContext context, CreateOrderRequest request);

    /**
     * Retrieves one order by ID while enforcing ownership/role access rules.
     *
     * @param context request actor context
     * @param orderId order identifier
     * @return order response
     */
    OrderResponse getOrderById(OrderRequestContext context, UUID orderId);

    /**
     * Lists orders for current customer actor.
     *
     * @param context request actor context
     * @return customer order history
     */
    List<OrderResponse> listCustomerOrders(OrderRequestContext context);

    /**
     * Lists admin-visible orders with optional filtering.
     *
     * @param context request actor context
     * @param pageable pagination config
     * @param query optional text query
     * @param status optional order status filter
     * @param refundStatus optional refund status filter
     * @return paged order list
     */
    Page<OrderResponse> listAdminOrders(
            OrderRequestContext context,
            Pageable pageable,
            String query,
            OrderStatus status,
            RefundStatus refundStatus
    );

    /**
     * Updates one order status with transition validation.
     *
     * @param context request actor context
     * @param orderId order identifier
     * @param request status update payload
     * @return updated order response
     */
    OrderResponse updateOrderStatus(OrderRequestContext context, UUID orderId, UpdateOrderStatusRequest request);

    /**
     * Returns status timeline events for one order.
     *
     * @param context request actor context
     * @param orderId order identifier
     * @return chronological timeline events
     */
    List<OrderStatusEventResponse> getOrderTimeline(OrderRequestContext context, UUID orderId);
}

