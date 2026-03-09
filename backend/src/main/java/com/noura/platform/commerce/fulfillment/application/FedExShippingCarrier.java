package com.noura.platform.commerce.fulfillment.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * FedEx adapter placeholder for local/demo builds.
 *
 * Real FedEx API integration previously drifted from the current ShippingCarrier API
 * and caused compilation failures. This adapter keeps the service wiring intact while
 * returning deterministic "not configured" responses unless properly enabled.
 */
@Component
public class FedExShippingCarrier implements ShippingCarrier {
    private static final Logger log = LoggerFactory.getLogger(FedExShippingCarrier.class);
    private static final String CARRIER_ID = "fedex";

    private final boolean enabled;
    private final String apiKey;
    private final String secretKey;
    private final String accountNumber;

    public FedExShippingCarrier(
            @Value("${app.shipping.fedex.enabled:false}") boolean enabled,
            @Value("${app.shipping.fedex.api-key:}") String apiKey,
            @Value("${app.shipping.fedex.secret-key:}") String secretKey,
            @Value("${app.shipping.fedex.account-number:}") String accountNumber
    ) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.accountNumber = accountNumber;
    }

    @Override
    public String getCarrierId() {
        return CARRIER_ID;
    }

    @Override
    public String getDisplayName() {
        return "FedEx";
    }

    @Override
    public boolean isEnabled() {
        return enabled && isConfigured();
    }

    @Override
    public List<ShippingRate> getRates(RateRequest request) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }
        log.warn("FedEx live rate lookup is not implemented in this build");
        return Collections.emptyList();
    }

    @Override
    public ShipmentResult createShipment(CreateShipmentRequest request) {
        if (!isEnabled()) {
            return ShipmentResult.failure("NOT_CONFIGURED", "FedEx is not configured");
        }
        log.warn("FedEx shipment creation is not implemented in this build");
        return ShipmentResult.failure("NOT_IMPLEMENTED", "FedEx shipment API is not implemented in this build");
    }

    @Override
    public TrackingInfo getTracking(String trackingNumber) {
        if (!isEnabled()) {
            return new TrackingInfo(
                    trackingNumber,
                    TrackingStatus.UNKNOWN,
                    "FedEx is not configured",
                    null,
                    null,
                    null,
                    List.of()
            );
        }
        log.warn("FedEx tracking is not implemented in this build");
        return new TrackingInfo(
                trackingNumber,
                TrackingStatus.UNKNOWN,
                "FedEx tracking API is not implemented in this build",
                null,
                null,
                null,
                List.of()
        );
    }

    @Override
    public CancelResult cancelShipment(String trackingNumber) {
        if (!isEnabled()) {
            return new CancelResult(false, "FedEx is not configured");
        }
        log.warn("FedEx cancellation is not implemented in this build");
        return new CancelResult(false, "FedEx cancellation API is not implemented in this build");
    }

    private boolean isConfigured() {
        return !isBlank(apiKey) && !isBlank(secretKey) && !isBlank(accountNumber);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
