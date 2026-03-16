package com.noura.platform.dto.fulfillment;

import java.time.LocalDateTime;

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
