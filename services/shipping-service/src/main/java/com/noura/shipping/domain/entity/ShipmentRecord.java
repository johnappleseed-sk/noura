package com.noura.shipping.domain.entity;

import com.noura.shipping.domain.enums.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Shipment aggregate root representing one shipment lifecycle record for an order.
 */
@Getter
@Setter
@Entity
@Table(name = "shipment_records")
public class ShipmentRecord extends AuditableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_number", length = 64)
    private String orderNumber;

    @Column(name = "customer_ref", nullable = false, length = 180)
    private String customerRef;

    @Column(name = "shipment_reference", nullable = false, length = 64, unique = true)
    private String shipmentReference;

    @Column(name = "carrier_code", nullable = false, length = 64)
    private String carrierCode;

    @Column(name = "method_code", nullable = false, length = 64)
    private String methodCode;

    @Column(name = "method_name", nullable = false, length = 120)
    private String methodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "quoted_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal quotedAmount;

    @Column(name = "currency_code", nullable = false, length = 8)
    private String currencyCode;

    @Column(name = "external_shipment_id", length = 128)
    private String externalShipmentId;

    @Column(name = "tracking_number", length = 128)
    private String trackingNumber;

    @Column(name = "tracking_url", length = 512)
    private String trackingUrl;

    @Column(name = "estimated_delivery_at")
    private Instant estimatedDeliveryAt;

    @Column(name = "label_created_at")
    private Instant labelCreatedAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "last_status_update_at")
    private Instant lastStatusUpdateAt;

    @Column(name = "last_carrier_sync_at")
    private Instant lastCarrierSyncAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "recipient_address_json", nullable = false)
    private String recipientAddressJson;

    @Column(name = "parcel_summary_json", nullable = false)
    private String parcelSummaryJson;

    @Column(name = "metadata_json")
    private String metadataJson;

    /**
     * Normalizes mutable fields before insert/update.
     */
    @PrePersist
    @PreUpdate
    protected void normalize() {
        orderNumber = trimToNull(orderNumber);
        customerRef = trimToNull(customerRef);
        shipmentReference = trimToNull(shipmentReference);
        carrierCode = normalizeCode(carrierCode);
        methodCode = normalizeCode(methodCode);
        methodName = trimToNull(methodName);
        idempotencyKey = trimToNull(idempotencyKey);
        currencyCode = normalizeCurrencyCode(currencyCode);
        externalShipmentId = trimToNull(externalShipmentId);
        trackingNumber = trimToNull(trackingNumber);
        trackingUrl = trimToNull(trackingUrl);
        failureReason = trimToNull(failureReason);
        recipientAddressJson = trimToNull(recipientAddressJson);
        parcelSummaryJson = trimToNull(parcelSummaryJson);
        metadataJson = trimToNull(metadataJson);

        if (status == null) {
            status = ShipmentStatus.CREATED;
        }
        if (quotedAmount == null) {
            quotedAmount = BigDecimal.ZERO;
        }
        if (lastStatusUpdateAt == null) {
            lastStatusUpdateAt = Instant.now();
        }
        if (status == ShipmentStatus.LABEL_CREATED && labelCreatedAt == null) {
            labelCreatedAt = Instant.now();
        }
        if (status == ShipmentStatus.IN_TRANSIT && shippedAt == null) {
            shippedAt = Instant.now();
        }
        if (status == ShipmentStatus.DELIVERED && deliveredAt == null) {
            deliveredAt = Instant.now();
        }
    }

    /**
     * Trims source text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Normalizes carrier and method codes to lowercase.
     *
     * @param value source code
     * @return normalized code
     */
    private String normalizeCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes currency code to uppercase, defaulting to USD when omitted.
     *
     * @param value source currency code
     * @return normalized currency code
     */
    private String normalizeCurrencyCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "USD";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
