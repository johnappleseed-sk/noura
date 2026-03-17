package com.noura.shipping.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.shipping.domain.entity.ShipmentRecord;
import com.noura.shipping.domain.enums.ShipmentStatus;
import com.noura.shipping.dto.shipping.CreateShipmentRequest;
import com.noura.shipping.dto.shipping.ParcelRequest;
import com.noura.shipping.dto.shipping.ShipmentResponse;
import com.noura.shipping.dto.shipping.ShipmentStatusUpdateRequest;
import com.noura.shipping.exception.ShippingOperationException;
import com.noura.shipping.integration.client.OrderServiceClient;
import com.noura.shipping.provider.ShippingCarrier;
import com.noura.shipping.provider.ShippingCarrierRegistry;
import com.noura.shipping.repository.ShipmentRecordRepository;
import com.noura.shipping.service.model.ShippingRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ShippingServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {

    @Mock
    private ShipmentRecordRepository shipmentRecordRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private ShippingCarrierRegistry shippingCarrierRegistry;

    @Mock
    private ShippingCarrier shippingCarrier;

    private ShippingServiceImpl shippingService;

    /**
     * Initializes the service under test before each test case.
     */
    @BeforeEach
    void setUp() {
        shippingService = new ShippingServiceImpl(
                shipmentRecordRepository,
                orderServiceClient,
                shippingCarrierRegistry,
                new ObjectMapper()
        );
    }

    /**
     * Verifies shipment-create retries reuse the existing idempotent shipment instead of calling the carrier again.
     */
    @Test
    void shouldReturnExistingShipmentWhenIdempotencyKeyMatches() {
        UUID orderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ShipmentRecord existing = shipment(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                orderId,
                "customer-1",
                ShipmentStatus.LABEL_CREATED
        );
        existing.setIdempotencyKey("idem-1");

        when(orderServiceClient.getOrderById(any(), any(), eq(orderId))).thenReturn(order(orderId, "customer-1"));
        when(shipmentRecordRepository.findByOrderIdAndCustomerRefAndIdempotencyKey(orderId, "customer-1", "idem-1"))
                .thenReturn(Optional.of(existing));

        ShipmentResponse response = shippingService.createShipment(
                new ShippingRequestContext("customer-1", null, Set.of(), false),
                new CreateShipmentRequest(
                        orderId,
                        "rule-based",
                        "standard",
                        "idem-1",
                        false,
                        List.of(new ParcelRequest(1, new BigDecimal("1.0000"), null, null, null)),
                        Map.of()
                )
        );

        Assertions.assertEquals(existing.getId(), response.id());
        verify(shippingCarrierRegistry, never()).resolve(any());
        verify(shipmentRecordRepository, never()).save(any(ShipmentRecord.class));
    }

    /**
     * Verifies shipment creation persists carrier-backed shipment records using order address and totals.
     */
    @Test
    void shouldCreateShipmentFromOrderSnapshot() {
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(orderServiceClient.getOrderById(any(), any(), eq(orderId))).thenReturn(order(orderId, "customer-2"));
        when(shipmentRecordRepository.findByOrderIdAndCustomerRefAndIdempotencyKey(orderId, "customer-2", "idem-2"))
                .thenReturn(Optional.empty());
        when(shipmentRecordRepository.findByOrderIdOrderByUpdatedAtDesc(orderId)).thenReturn(List.of());
        when(shippingCarrierRegistry.resolve("rule-based")).thenReturn(shippingCarrier);
        when(shippingCarrier.carrierCode()).thenReturn("rule-based");
        when(shippingCarrier.quote(any())).thenReturn(new ShippingCarrier.QuoteResult(
                "rule-based",
                "express",
                "Express Shipping",
                new BigDecimal("9.9900"),
                "USD",
                1,
                2,
                Instant.now().plusSeconds(7200),
                "Express shipping quote"
        ));
        when(shippingCarrier.createShipment(any())).thenReturn(new ShippingCarrier.ShipmentCreationResult(
                ShipmentStatus.LABEL_CREATED,
                "ship_external_123",
                "NRA123456789012",
                "https://tracking.noura.local/track/NRA123456789012",
                Instant.now().plusSeconds(7200),
                Instant.now(),
                null
        ));
        when(shipmentRecordRepository.save(any(ShipmentRecord.class))).thenAnswer(invocation -> {
            ShipmentRecord shipment = invocation.getArgument(0, ShipmentRecord.class);
            shipment.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
            shipment.setCreatedAt(Instant.now());
            shipment.setUpdatedAt(Instant.now());
            return shipment;
        });

        ShipmentResponse response = shippingService.createShipment(
                new ShippingRequestContext("customer-2", null, Set.of(), false),
                new CreateShipmentRequest(
                        orderId,
                        "rule-based",
                        "express",
                        "idem-2",
                        false,
                        List.of(new ParcelRequest(2, new BigDecimal("0.7500"), null, null, null)),
                        Map.of("shippingScenario", "ready")
                )
        );

        Assertions.assertEquals(orderId, response.orderId());
        Assertions.assertEquals("express", response.methodCode());
        Assertions.assertEquals(ShipmentStatus.LABEL_CREATED, response.status());
        Assertions.assertEquals("USD", response.currencyCode());
        Assertions.assertEquals("KH", response.recipientAddress().countryCode());
        Assertions.assertEquals("NRA123456789012", response.trackingNumber());
    }

    /**
     * Verifies invalid terminal-to-nonterminal manual transitions are rejected.
     */
    @Test
    void shouldRejectInvalidManualStatusTransition() {
        UUID shipmentId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        ShipmentRecord existing = shipment(shipmentId, UUID.randomUUID(), "customer-3", ShipmentStatus.DELIVERED);
        existing.setDeliveredAt(Instant.now());

        when(shipmentRecordRepository.findByIdForUpdate(shipmentId)).thenReturn(Optional.of(existing));

        ShippingOperationException exception = Assertions.assertThrows(
                ShippingOperationException.class,
                () -> shippingService.updateShipmentStatus(
                        new ShipmentStatusUpdateRequest(
                                shipmentId,
                                ShipmentStatus.IN_TRANSIT,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "internal",
                                null,
                                Instant.now()
                        ),
                        "internal"
                )
        );

        Assertions.assertEquals("SHIPMENT_STATUS_INVALID_TRANSITION", exception.getCode());
        verify(shipmentRecordRepository, never()).save(any(ShipmentRecord.class));
    }

    /**
     * Builds an order payload with a structured shipping address.
     *
     * @param orderId order identifier
     * @param customerRef customer reference
     * @return order payload
     */
    private OrderServiceClient.OrderPayload order(UUID orderId, String customerRef) {
        return new OrderServiceClient.OrderPayload(
                orderId,
                "ORD-20260317-ABC12345",
                customerRef,
                new BigDecimal("49.9900"),
                new BigDecimal("0.0000"),
                new BigDecimal("49.9900"),
                "USD",
                new ObjectMapper().valueToTree(Map.of(
                        "fullName", "Customer Example",
                        "phone", "012345678",
                        "line1", "Street 1",
                        "city", "Phnom Penh",
                        "postalCode", "12000",
                        "countryCode", "KH"
                )),
                "PAID"
        );
    }

    /**
     * Builds a shipment aggregate for tests.
     *
     * @param shipmentId shipment identifier
     * @param orderId order identifier
     * @param customerRef customer reference
     * @param status shipment status
     * @return shipment aggregate
     */
    private ShipmentRecord shipment(UUID shipmentId, UUID orderId, String customerRef, ShipmentStatus status) {
        ShipmentRecord shipment = new ShipmentRecord();
        shipment.setId(shipmentId);
        shipment.setOrderId(orderId);
        shipment.setOrderNumber("ORD-TEST");
        shipment.setCustomerRef(customerRef);
        shipment.setShipmentReference("SHP-TEST");
        shipment.setCarrierCode("rule-based");
        shipment.setMethodCode("standard");
        shipment.setMethodName("Standard Shipping");
        shipment.setStatus(status);
        shipment.setQuotedAmount(new BigDecimal("4.9900"));
        shipment.setCurrencyCode("USD");
        shipment.setRecipientAddressJson("{\"countryCode\":\"KH\"}");
        shipment.setParcelSummaryJson("[{\"quantity\":1,\"weightKg\":1.0}]");
        shipment.setCreatedAt(Instant.now());
        shipment.setUpdatedAt(Instant.now());
        return shipment;
    }
}
