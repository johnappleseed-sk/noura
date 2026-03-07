package com.noura.platform.commerce.fulfillment.domain;

import com.noura.platform.commerce.orders.domain.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "customer_order_shipment")
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(length = 120)
    private String carrier;

    @Column(name = "tracking_number", length = 128)
    private String trackingNumber;

    @Column(name = "tracking_url", length = 512)
    private String trackingUrl;

    @Column(name = "estimated_delivery_at")
    private LocalDateTime estimatedDeliveryAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(length = 600)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        normalize();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (carrier != null) {
            carrier = carrier.trim();
            if (carrier.isBlank()) {
                carrier = null;
            }
        }
        if (trackingNumber != null) {
            trackingNumber = trackingNumber.trim();
            if (trackingNumber.isBlank()) {
                trackingNumber = null;
            }
        }
        if (trackingUrl != null) {
            trackingUrl = trackingUrl.trim();
            if (trackingUrl.isBlank()) {
                trackingUrl = null;
            }
        }
        if (notes != null) {
            notes = notes.trim();
            if (notes.isBlank()) {
                notes = null;
            } else if (notes.length() > 600) {
                notes = notes.substring(0, 600);
            }
        }
        if (status == null) {
            status = ShipmentStatus.PENDING;
        }
    }
}
