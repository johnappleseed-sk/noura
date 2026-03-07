package com.noura.platform.commerce.fulfillment.application;

import com.noura.platform.commerce.fulfillment.domain.Shipment;
import com.noura.platform.commerce.fulfillment.domain.ShipmentStatus;
import com.noura.platform.commerce.fulfillment.infrastructure.ShipmentRepo;
import com.noura.platform.commerce.orders.domain.Order;
import com.noura.platform.commerce.orders.domain.OrderStatus;
import com.noura.platform.commerce.orders.infrastructure.OrderRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Service
@Transactional
public class StorefrontFulfillmentService {
    private final ShipmentRepo shipmentRepo;
    private final OrderRepo orderRepo;

    public StorefrontFulfillmentService(ShipmentRepo shipmentRepo, OrderRepo orderRepo) {
        this.shipmentRepo = shipmentRepo;
        this.orderRepo = orderRepo;
    }

    @Transactional(readOnly = true)
    public ShipmentDto getLatestForOrder(Long customerId, Long orderId) {
        resolveOrder(orderId, customerId);
        return shipmentRepo.findFirstByOrder_IdOrderByCreatedAtDesc(orderId)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ShipmentDto getLatestForOrderStaff(Long orderId) {
        resolveOrderById(orderId);
        return shipmentRepo.findFirstByOrder_IdOrderByCreatedAtDesc(orderId)
                .map(this::toDto)
                .orElse(null);
    }

    public ShipmentDto update(Long customerId, Long orderId, UpsertShipmentRequest request) {
        Order order = resolveOrder(orderId, customerId);
        return updateShipment(order, request);
    }

    public ShipmentDto updateAsStaff(Long orderId, UpsertShipmentRequest request) {
        Order order = resolveOrderById(orderId);
        return updateShipment(order, request);
    }

    private Order resolveOrder(Long orderId, Long customerId) {
        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order id.");
        }
        return orderRepo.findById(orderId)
                .filter(order -> order.getCustomerAccount() != null
                        && customerId != null
                        && customerId.equals(order.getCustomerAccount().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
    }

    private Order resolveOrderById(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order id.");
        }
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
    }

    private ShipmentStatus normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (normalized.isBlank()) {
            return ShipmentStatus.PENDING;
        }
        try {
            return ShipmentStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipment status.");
        }
    }

    private String normalizeText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid estimatedDeliveryAt format.");
        }
    }

    private ShipmentDto toDto(Shipment shipment) {
        return new ShipmentDto(
                shipment.getId(),
                shipment.getStatus().name(),
                shipment.getCarrier(),
                shipment.getTrackingNumber(),
                shipment.getTrackingUrl(),
                shipment.getEstimatedDeliveryAt(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt(),
                shipment.getNotes(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }

    private ShipmentDto updateShipment(Order order, UpsertShipmentRequest request) {
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update fulfillment for a completed order.");
        }

        Shipment shipment = shipmentRepo.findTopByOrder_IdOrderByCreatedAtDesc(order.getId())
                .orElseGet(() -> {
                    Shipment created = new Shipment();
                    created.setOrder(order);
                    return created;
                });

        ShipmentStatus newStatus = normalizeStatus(request == null ? null : request.status());
        shipment.setStatus(newStatus);
        shipment.setCarrier(normalizeText(request == null ? null : request.carrier(), 120));
        shipment.setTrackingNumber(normalizeText(request == null ? null : request.trackingNumber(), 128));
        shipment.setTrackingUrl(normalizeText(request == null ? null : request.trackingUrl(), 512));
        shipment.setNotes(truncate(request == null ? null : request.notes(), 600));
        shipment.setEstimatedDeliveryAt(parseDateTime(request == null ? null : request.estimatedDeliveryAt()));

        LocalDateTime now = LocalDateTime.now();
        if (newStatus == ShipmentStatus.SHIPPED && shipment.getShippedAt() == null) {
            shipment.setShippedAt(now);
        }
        if (newStatus == ShipmentStatus.DELIVERED && shipment.getDeliveredAt() == null) {
            shipment.setDeliveredAt(now);
            if (order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.PAID) {
                order.setStatus(OrderStatus.FULFILLED);
            }
        }
        if (newStatus == ShipmentStatus.CANCELLED && shipment.getDeliveredAt() == null) {
            shipment.setDeliveredAt(null);
            if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.REFUNDED) {
                order.setStatus(OrderStatus.CANCELLED);
            }
        }
        if (newStatus == ShipmentStatus.SHIPPED && (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PENDING_PAYMENT)) {
            order.setStatus(OrderStatus.PROCESSING);
        }
        if (newStatus == ShipmentStatus.READY && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PAID);
        }

        Shipment saved = shipmentRepo.save(shipment);
        return toDto(saved);
    }

    public record UpsertShipmentRequest(String status, String carrier, String trackingNumber, String trackingUrl,
                                        String estimatedDeliveryAt, String notes) {
    }

    public record ShipmentDto(Long id,
                             String status,
                             String carrier,
                             String trackingNumber,
                             String trackingUrl,
                             LocalDateTime estimatedDeliveryAt,
                             LocalDateTime shippedAt,
                             LocalDateTime deliveredAt,
                             String notes,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
    }
}
