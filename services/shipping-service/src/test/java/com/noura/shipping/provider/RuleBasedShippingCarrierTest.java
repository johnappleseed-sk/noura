package com.noura.shipping.provider;

import com.noura.shipping.config.RuleBasedCarrierProperties;
import com.noura.shipping.domain.enums.ShipmentStatus;
import com.noura.shipping.exception.ShippingOperationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unit tests for {@link RuleBasedShippingCarrier}.
 */
class RuleBasedShippingCarrierTest {

    /**
     * Verifies standard shipping becomes free when the configured subtotal threshold is reached.
     */
    @Test
    void shouldReturnFreeStandardShippingAtThreshold() {
        RuleBasedShippingCarrier carrier = new RuleBasedShippingCarrier(new RuleBasedCarrierProperties());

        ShippingCarrier.QuoteResult result = carrier.quote(
                new ShippingCarrier.QuoteRequest(
                        address("Phnom Penh", "KH"),
                        new BigDecimal("75.0000"),
                        "USD",
                        2,
                        new BigDecimal("1.5000"),
                        "standard",
                        Map.of()
                )
        );

        Assertions.assertEquals(new BigDecimal("0.0000"), result.amount());
        Assertions.assertEquals("standard", result.methodCode());
    }

    /**
     * Verifies same-day shipping is offered only for configured cities.
     */
    @Test
    void shouldOfferSameDayOnlyForConfiguredCity() {
        RuleBasedShippingCarrier carrier = new RuleBasedShippingCarrier(new RuleBasedCarrierProperties());

        List<ShippingCarrier.AvailableMethod> phnomPenhMethods = carrier.listAvailableMethods(
                new ShippingCarrier.MethodRequest(
                        address("Phnom Penh", "KH"),
                        new BigDecimal("20.0000"),
                        "USD",
                        1,
                        new BigDecimal("0.7000"),
                        Map.of()
                )
        );
        List<ShippingCarrier.AvailableMethod> battambangMethods = carrier.listAvailableMethods(
                new ShippingCarrier.MethodRequest(
                        address("Battambang", "KH"),
                        new BigDecimal("20.0000"),
                        "USD",
                        1,
                        new BigDecimal("0.7000"),
                        Map.of()
                )
        );

        Assertions.assertTrue(phnomPenhMethods.stream().anyMatch(method -> "same_day".equals(method.methodCode())));
        Assertions.assertTrue(battambangMethods.stream().noneMatch(method -> "same_day".equals(method.methodCode())));
    }

    /**
     * Verifies default shipment creation returns label and tracking data.
     */
    @Test
    void shouldCreateLabelTrackedShipmentByDefault() {
        RuleBasedShippingCarrier carrier = new RuleBasedShippingCarrier(new RuleBasedCarrierProperties());

        ShippingCarrier.ShipmentCreationResult result = carrier.createShipment(
                new ShippingCarrier.CreateShipmentCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "ORD-123",
                        "SHP-20260317-ABC12345",
                        address("Phnom Penh", "KH"),
                        List.of(new ShippingCarrier.Parcel(1, new BigDecimal("1.2000"), null, null, null)),
                        new BigDecimal("30.0000"),
                        "USD",
                        "express",
                        false,
                        Map.of()
                )
        );

        Assertions.assertEquals(ShipmentStatus.LABEL_CREATED, result.status());
        Assertions.assertNotNull(result.trackingNumber());
        Assertions.assertTrue(result.trackingNumber().startsWith("NRA"));
        Assertions.assertNotNull(result.labelCreatedAt());
    }

    /**
     * Verifies unsupported or unavailable methods are rejected for quotes.
     */
    @Test
    void shouldRejectUnavailableMethodQuote() {
        RuleBasedShippingCarrier carrier = new RuleBasedShippingCarrier(new RuleBasedCarrierProperties());

        ShippingOperationException exception = Assertions.assertThrows(
                ShippingOperationException.class,
                () -> carrier.quote(
                        new ShippingCarrier.QuoteRequest(
                                address("New York", "US"),
                                new BigDecimal("10.0000"),
                                "USD",
                                1,
                                new BigDecimal("12.0000"),
                                "same_day",
                                Map.of()
                        )
                )
        );

        Assertions.assertEquals("SHIPPING_METHOD_UNAVAILABLE", exception.getCode());
    }

    /**
     * Builds a minimal carrier address for tests.
     *
     * @param city city name
     * @param countryCode country code
     * @return carrier address
     */
    private ShippingCarrier.ShippingAddress address(String city, String countryCode) {
        return new ShippingCarrier.ShippingAddress(
                "Test Recipient",
                "012345678",
                "Street 1",
                null,
                null,
                city,
                null,
                "12000",
                countryCode
        );
    }
}
