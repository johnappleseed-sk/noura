package com.noura.platform.service;

import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.OrderTimelineEventDto;
import com.noura.platform.dto.order.UpdateOrderStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    /**
     * Executes admin orders.
     *
     * @param pageable The pagination configuration.
     * @return A paginated result set.
     */
    Page<OrderDto> adminOrders(Pageable pageable);

    /**
     * Retrieves by id.
     *
     * @param orderId The order id used to locate the target record.
     * @return The mapped DTO representation.
     */
    OrderDto getById(UUID orderId);

    /**
     * Retrieves order timeline.
     *
     * @param orderId The order id used to locate the target record.
     * @return A list of matching items.
     */
    List<OrderTimelineEventDto> orderTimeline(UUID orderId);

    /**
     * Updates status.
     *
     * @param orderId The order id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    OrderDto updateStatus(UUID orderId, UpdateOrderStatusRequest request);
}
