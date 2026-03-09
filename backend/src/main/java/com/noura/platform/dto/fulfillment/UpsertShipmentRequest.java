package com.noura.platform.dto.fulfillment;

public record UpsertShipmentRequest(String status, String carrier, String trackingNumber, String trackingUrl,
                                    String estimatedDeliveryAt, String notes) {
}
