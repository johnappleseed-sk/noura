package com.noura.platform.commerce.fulfillment.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Stub shipping carrier for development and testing.
 * Returns mock rates and tracking - replace with real carrier implementations in production.
 */
@Component
public class StubShippingCarrier implements ShippingCarrier {
    private static final Logger log = LoggerFactory.getLogger(StubShippingCarrier.class);
    private static final String CARRIER_ID = "stub";

    @Override
    public String getCarrierId() {
        return CARRIER_ID;
    }

    @Override
    public String getDisplayName() {
        return "Stub Carrier (Dev)";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<ShippingRate> getRates(RateRequest request) {
        log.info("STUB: Getting rates for shipment to {}", request.toAddress().city());

        return List.of(
                new ShippingRate(
                        "STANDARD",
                        "Standard Shipping",
                        new BigDecimal("9.99"),
                        "USD",
                        5,
                        7,
                        LocalDateTime.now().plusDays(7)
                ),
                new ShippingRate(
                        "EXPRESS",
                        "Express Shipping",
                        new BigDecimal("19.99"),
                        "USD",
                        2,
                        3,
                        LocalDateTime.now().plusDays(3)
                ),
                new ShippingRate(
                        "OVERNIGHT",
                        "Overnight Shipping",
                        new BigDecimal("39.99"),
                        "USD",
                        1,
                        1,
                        LocalDateTime.now().plusDays(1)
                )
        );
    }

    @Override
    public ShipmentResult createShipment(CreateShipmentRequest request) {
        log.info("STUB: Creating shipment to {} with service {}",
                request.toAddress().city(), request.serviceCode());

        String trackingNumber = "STUB" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        BigDecimal cost = switch (request.serviceCode()) {
            case "EXPRESS" -> new BigDecimal("19.99");
            case "OVERNIGHT" -> new BigDecimal("39.99");
            default -> new BigDecimal("9.99");
        };

        int days = switch (request.serviceCode()) {
            case "EXPRESS" -> 3;
            case "OVERNIGHT" -> 1;
            default -> 7;
        };

        return ShipmentResult.success(
                trackingNumber,
                "https://example.com/labels/" + trackingNumber + ".pdf",
                cost,
                "USD",
                LocalDateTime.now().plusDays(days)
        );
    }

    @Override
    public TrackingInfo getTracking(String trackingNumber) {
        log.info("STUB: Getting tracking for {}", trackingNumber);

        // Return mock tracking showing package in transit
        return new TrackingInfo(
                trackingNumber,
                TrackingStatus.IN_TRANSIT,
                "Package is in transit",
                LocalDateTime.now().plusDays(2),
                null,
                null,
                List.of(
                        new TrackingEvent(
                                LocalDateTime.now().minusDays(1),
                                "Origin Facility",
                                "Package received at origin facility",
                                TrackingStatus.PICKED_UP
                        ),
                        new TrackingEvent(
                                LocalDateTime.now().minusHours(12),
                                "Sort Facility",
                                "Package departed sort facility",
                                TrackingStatus.IN_TRANSIT
                        ),
                        new TrackingEvent(
                                LocalDateTime.now().minusHours(2),
                                "Distribution Center",
                                "Package arrived at distribution center",
                                TrackingStatus.IN_TRANSIT
                        )
                )
        );
    }

    @Override
    public CancelResult cancelShipment(String trackingNumber) {
        log.info("STUB: Cancelling shipment {}", trackingNumber);

        return new CancelResult(true, "Shipment cancelled successfully");
    }
}
