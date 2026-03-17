package com.noura.shipping.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.shipping.domain.entity.ShipmentRecord;
import com.noura.shipping.domain.enums.FulfillmentHookType;
import com.noura.shipping.domain.enums.ShipmentStatus;
import com.noura.shipping.dto.shipping.AddressRequest;
import com.noura.shipping.dto.shipping.CreateShipmentRequest;
import com.noura.shipping.dto.shipping.ParcelRequest;
import com.noura.shipping.dto.shipping.ShipmentResponse;
import com.noura.shipping.dto.shipping.ShipmentStatusUpdateRequest;
import com.noura.shipping.dto.shipping.ShippingMethodQueryRequest;
import com.noura.shipping.dto.shipping.ShippingMethodResponse;
import com.noura.shipping.dto.shipping.ShippingQuoteRequest;
import com.noura.shipping.dto.shipping.ShippingQuoteResponse;
import com.noura.shipping.exception.NotFoundException;
import com.noura.shipping.exception.ShippingOperationException;
import com.noura.shipping.integration.client.OrderServiceClient;
import com.noura.shipping.provider.ShippingCarrier;
import com.noura.shipping.provider.ShippingCarrierRegistry;
import com.noura.shipping.repository.ShipmentRecordRepository;
import com.noura.shipping.service.ShippingService;
import com.noura.shipping.service.model.ShippingRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of {@link ShippingService}.
 *
 * <p>The service owns shipment state, status transition validation, rule-based quote orchestration,
 * read-only order validation, and future-ready carrier adapter integration points.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final TypeReference<List<ParcelRequest>> PARCEL_LIST_TYPE = new TypeReference<>() {
    };

    private static final DateTimeFormatter SHIPMENT_REFERENCE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> STATUS_FLOW = Map.of(
            ShipmentStatus.CREATED, Set.of(ShipmentStatus.LABEL_CREATED, ShipmentStatus.READY_FOR_FULFILLMENT, ShipmentStatus.IN_TRANSIT, ShipmentStatus.EXCEPTION, ShipmentStatus.CANCELLED),
            ShipmentStatus.LABEL_CREATED, Set.of(ShipmentStatus.READY_FOR_FULFILLMENT, ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED, ShipmentStatus.EXCEPTION, ShipmentStatus.CANCELLED),
            ShipmentStatus.READY_FOR_FULFILLMENT, Set.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED, ShipmentStatus.EXCEPTION, ShipmentStatus.CANCELLED),
            ShipmentStatus.IN_TRANSIT, Set.of(ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED, ShipmentStatus.RETURNED, ShipmentStatus.EXCEPTION),
            ShipmentStatus.OUT_FOR_DELIVERY, Set.of(ShipmentStatus.DELIVERED, ShipmentStatus.RETURNED, ShipmentStatus.EXCEPTION),
            ShipmentStatus.EXCEPTION, Set.of(ShipmentStatus.READY_FOR_FULFILLMENT, ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED, ShipmentStatus.RETURNED, ShipmentStatus.CANCELLED),
            ShipmentStatus.DELIVERED, Set.of(ShipmentStatus.RETURNED),
            ShipmentStatus.RETURNED, Set.of(),
            ShipmentStatus.CANCELLED, Set.of()
    );

    private final ShipmentRecordRepository shipmentRecordRepository;
    private final OrderServiceClient orderServiceClient;
    private final ShippingCarrierRegistry shippingCarrierRegistry;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ShippingMethodResponse> getAvailableMethods(ShippingRequestContext context, ShippingMethodQueryRequest request) {
        ShippingCarrier.MethodRequest methodRequest = new ShippingCarrier.MethodRequest(
                new ShippingCarrier.ShippingAddress(
                        null,
                        null,
                        null,
                        null,
                        null,
                        normalizeNullable(request.getCity()),
                        normalizeNullable(request.getStateProvince()),
                        normalizeNullable(request.getPostalCode()),
                        normalizeCountryCode(request.getCountryCode())
                ),
                normalizeMoney(request.getCartSubtotal()),
                normalizeCurrencyCode(request.getCurrencyCode()),
                request.getItemCount(),
                normalizeWeight(request.getTotalWeightKg()),
                Map.of()
        );
        return shippingCarrierRegistry.resolveAll(request.getCarrierCode()).stream()
                .flatMap(carrier -> carrier.listAvailableMethods(methodRequest).stream())
                .map(method -> new ShippingMethodResponse(
                        method.carrierCode(),
                        method.methodCode(),
                        method.methodName(),
                        method.amount(),
                        method.currencyCode(),
                        method.estimatedDaysMin(),
                        method.estimatedDaysMax(),
                        method.estimatedDeliveryAt(),
                        method.supportsTracking(),
                        method.ruleSummary()
                ))
                .sorted((left, right) -> left.amount().compareTo(right.amount()))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ShippingQuoteResponse quote(ShippingRequestContext context, ShippingQuoteRequest request) {
        ShippingCarrier carrier = shippingCarrierRegistry.resolve(request.carrierCode());
        ShippingCarrier.QuoteResult quote = carrier.quote(toQuoteRequest(request));
        return toQuoteResponse(quote);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ShipmentResponse createShipment(ShippingRequestContext context, CreateShipmentRequest request) {
        OrderServiceClient.OrderPayload order = orderServiceClient.getOrderById(context, currentCorrelationId(), request.orderId());
        assertOrderAccess(context, order);
        assertOrderSupportsShipment(order);

        String customerRef = normalizeNullable(order.customerRef());
        String idempotencyKey = normalizeNullable(request.idempotencyKey());
        if (idempotencyKey != null) {
            ShipmentRecord existing = shipmentRecordRepository
                    .findByOrderIdAndCustomerRefAndIdempotencyKey(order.id(), customerRef, idempotencyKey)
                    .orElse(null);
            if (existing != null) {
                return toShipmentResponse(existing);
            }
        }

        ShipmentRecord latestExisting = shipmentRecordRepository.findByOrderIdOrderByUpdatedAtDesc(order.id()).stream()
                .findFirst()
                .orElse(null);
        // v1 keeps shipment ownership simple: one active shipment per order until split shipments are designed explicitly.
        if (latestExisting != null && !canCreateReplacementShipment(latestExisting)) {
            throw new ShippingOperationException(
                    HttpStatus.CONFLICT,
                    "SHIPMENT_ALREADY_EXISTS_FOR_ORDER",
                    "A shipment already exists for this order in the current first-slice design"
            );
        }

        ShippingCarrier carrier = shippingCarrierRegistry.resolve(request.carrierCode());
        AddressRequest recipientAddress = resolveRecipientAddress(order.shippingAddress());
        List<ParcelRequest> parcels = request.parcels();
        Map<String, Object> metadata = parseMetadata(request.metadata());
        BigDecimal subtotal = normalizeMoney(order.subtotal() == null ? order.totalAmount() : order.subtotal());

        ShippingCarrier.QuoteResult quote = carrier.quote(new ShippingCarrier.QuoteRequest(
                toCarrierAddress(recipientAddress),
                subtotal,
                normalizeCurrencyCode(order.currencyCode()),
                totalItemCount(parcels),
                totalWeight(parcels),
                normalizeMethodCode(request.methodCode()),
                metadata
        ));

        String shipmentReference = generateShipmentReference();
        ShippingCarrier.ShipmentCreationResult creationResult = carrier.createShipment(
                new ShippingCarrier.CreateShipmentCommand(
                        null,
                        order.id(),
                        normalizeNullable(order.orderNumber()),
                        shipmentReference,
                        toCarrierAddress(recipientAddress),
                        parcels.stream().map(this::toCarrierParcel).toList(),
                        subtotal,
                        normalizeCurrencyCode(order.currencyCode()),
                        normalizeMethodCode(request.methodCode()),
                        Boolean.TRUE.equals(request.signatureRequired()),
                        metadata
                )
        );

        ShipmentRecord shipment = new ShipmentRecord();
        shipment.setOrderId(order.id());
        shipment.setOrderNumber(normalizeNullable(order.orderNumber()));
        shipment.setCustomerRef(customerRef);
        shipment.setShipmentReference(shipmentReference);
        shipment.setCarrierCode(carrier.carrierCode());
        shipment.setMethodCode(quote.methodCode());
        shipment.setMethodName(quote.methodName());
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setIdempotencyKey(idempotencyKey);
        shipment.setQuotedAmount(quote.amount());
        shipment.setCurrencyCode(quote.currencyCode());
        shipment.setRecipientAddressJson(writeJson(recipientAddress));
        shipment.setParcelSummaryJson(writeJson(parcels));
        shipment.setMetadataJson(writeJson(metadata));
        shipment.setCreatedBy(context.actorId());
        shipment.setUpdatedBy(context.actorId());

        applyCarrierCreationResult(shipment, creationResult, context.actorId());

        try {
            ShipmentRecord saved = shipmentRecordRepository.save(shipment);
            log.info("Created shipment {} for order {} using carrier {}",
                    saved.getShipmentReference(), saved.getOrderId(), saved.getCarrierCode());
            return toShipmentResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            ShipmentRecord existing = idempotencyKey == null
                    ? null
                    : shipmentRecordRepository.findByOrderIdAndCustomerRefAndIdempotencyKey(order.id(), customerRef, idempotencyKey)
                    .orElse(null);
            if (existing != null) {
                return toShipmentResponse(existing);
            }
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ShipmentResponse getShipmentById(ShippingRequestContext context, UUID shipmentId, boolean refreshCarrier) {
        ShipmentRecord shipment = shipmentRecordRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new NotFoundException("SHIPMENT_NOT_FOUND", "Shipment not found"));
        assertShipmentAccess(context, shipment);
        if (refreshCarrier) {
            refreshCarrierStatus(shipment, context.actorId());
        }
        return toShipmentResponse(shipment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ShipmentResponse getLatestShipmentByOrderId(ShippingRequestContext context, UUID orderId, boolean refreshCarrier) {
        ShipmentRecord shipment = shipmentRecordRepository.findByOrderIdOrderByUpdatedAtDesc(orderId).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("SHIPMENT_NOT_FOUND", "Shipment not found for order"));
        assertShipmentAccess(context, shipment);
        if (refreshCarrier) {
            refreshCarrierStatus(shipment, context.actorId());
        }
        return toShipmentResponse(shipment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ShipmentResponse updateShipmentStatus(ShipmentStatusUpdateRequest request, String actor) {
        ShipmentRecord shipment = shipmentRecordRepository.findByIdForUpdate(request.shipmentId())
                .orElseThrow(() -> new NotFoundException("SHIPMENT_NOT_FOUND", "Shipment not found"));

        Map<String, Object> metadata = readMetadata(shipment.getMetadataJson());
        metadata.putAll(parseMetadata(request.metadata()));
        shipment.setMetadataJson(writeJson(metadata));

        applyStatusUpdate(
                shipment,
                request.status(),
                actor,
                request.externalShipmentId(),
                request.trackingNumber(),
                request.trackingUrl(),
                request.estimatedDeliveryAt(),
                request.failureReason(),
                request.eventTimestamp()
        );

        ShipmentRecord saved = shipmentRecordRepository.save(shipment);
        log.info("Updated shipment {} status to {} via source {}",
                saved.getShipmentReference(), saved.getStatus(), normalizeNullable(request.source()));
        return toShipmentResponse(saved);
    }

    /**
     * Refreshes shipment status from the carrier adapter when the shipment is not terminal.
     *
     * @param shipment shipment record
     * @param actor actor identifier
     */
    private void refreshCarrierStatus(ShipmentRecord shipment, String actor) {
        if (isTerminal(shipment.getStatus())) {
            return;
        }
        ShippingCarrier carrier = shippingCarrierRegistry.resolve(shipment.getCarrierCode());
        // The built-in rule-based carrier only advances when metadata or internal hooks say it should,
        // which keeps reads deterministic until real callbacks or polling jobs exist.
        ShippingCarrier.TrackingResult trackingResult = carrier.fetchShipmentStatus(
                new ShippingCarrier.StatusRequest(
                        shipment.getId(),
                        shipment.getShipmentReference(),
                        shipment.getExternalShipmentId(),
                        shipment.getTrackingNumber(),
                        shipment.getStatus(),
                        readMetadata(shipment.getMetadataJson())
                )
        );
        shipment.setLastCarrierSyncAt(Instant.now());
        applyStatusUpdate(
                shipment,
                trackingResult.status(),
                actor,
                shipment.getExternalShipmentId(),
                trackingResult.trackingNumber(),
                trackingResult.trackingUrl(),
                trackingResult.estimatedDeliveryAt(),
                trackingResult.failureReason(),
                trackingResult.deliveredAt()
        );
        shipmentRecordRepository.save(shipment);
    }

    /**
     * Applies one carrier shipment-creation result to the local aggregate.
     *
     * @param shipment target shipment
     * @param creationResult carrier creation result
     * @param actor actor identifier
     */
    private void applyCarrierCreationResult(
            ShipmentRecord shipment,
            ShippingCarrier.ShipmentCreationResult creationResult,
            String actor
    ) {
        shipment.setExternalShipmentId(normalizeNullable(creationResult.externalShipmentId()));
        shipment.setTrackingNumber(normalizeNullable(creationResult.trackingNumber()));
        shipment.setTrackingUrl(normalizeNullable(creationResult.trackingUrl()));
        shipment.setEstimatedDeliveryAt(creationResult.estimatedDeliveryAt());
        shipment.setLabelCreatedAt(creationResult.labelCreatedAt());
        shipment.setFailureReason(normalizeNullable(creationResult.failureReason()));
        applyStatusUpdate(
                shipment,
                creationResult.status(),
                actor,
                creationResult.externalShipmentId(),
                creationResult.trackingNumber(),
                creationResult.trackingUrl(),
                creationResult.estimatedDeliveryAt(),
                creationResult.failureReason(),
                creationResult.labelCreatedAt()
        );
    }

    /**
     * Applies a validated shipment status transition and related timestamps.
     *
     * @param shipment target shipment
     * @param nextStatus next status
     * @param actor actor identifier
     * @param externalShipmentId external shipment ID
     * @param trackingNumber tracking number
     * @param trackingUrl tracking URL
     * @param estimatedDeliveryAt estimated delivery timestamp
     * @param failureReason failure reason
     * @param eventTimestamp source event timestamp
     */
    private void applyStatusUpdate(
            ShipmentRecord shipment,
            ShipmentStatus nextStatus,
            String actor,
            String externalShipmentId,
            String trackingNumber,
            String trackingUrl,
            Instant estimatedDeliveryAt,
            String failureReason,
            Instant eventTimestamp
    ) {
        ShipmentStatus currentStatus = shipment.getStatus();
        ShipmentStatus targetStatus = nextStatus == null ? currentStatus : nextStatus;
        validateStatusTransition(currentStatus, targetStatus);

        Instant effectiveTimestamp = eventTimestamp != null ? eventTimestamp : Instant.now();
        shipment.setStatus(targetStatus);
        shipment.setExternalShipmentId(normalizeNullable(externalShipmentId) != null
                ? normalizeNullable(externalShipmentId)
                : shipment.getExternalShipmentId());
        shipment.setTrackingNumber(normalizeNullable(trackingNumber) != null
                ? normalizeNullable(trackingNumber)
                : shipment.getTrackingNumber());
        shipment.setTrackingUrl(normalizeNullable(trackingUrl) != null
                ? normalizeNullable(trackingUrl)
                : shipment.getTrackingUrl());
        shipment.setEstimatedDeliveryAt(estimatedDeliveryAt != null ? estimatedDeliveryAt : shipment.getEstimatedDeliveryAt());
        shipment.setLastStatusUpdateAt(effectiveTimestamp);
        shipment.setUpdatedBy(actor);

        if (targetStatus == ShipmentStatus.LABEL_CREATED && shipment.getLabelCreatedAt() == null) {
            shipment.setLabelCreatedAt(effectiveTimestamp);
        }
        if ((targetStatus == ShipmentStatus.IN_TRANSIT || targetStatus == ShipmentStatus.OUT_FOR_DELIVERY)
                && shipment.getShippedAt() == null) {
            shipment.setShippedAt(effectiveTimestamp);
        }
        if (targetStatus == ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(effectiveTimestamp);
        }
        if (targetStatus == ShipmentStatus.EXCEPTION || targetStatus == ShipmentStatus.RETURNED) {
            shipment.setFailureReason(normalizeNullable(failureReason));
        } else if (normalizeNullable(failureReason) != null) {
            shipment.setFailureReason(normalizeNullable(failureReason));
        } else if (targetStatus != ShipmentStatus.EXCEPTION) {
            shipment.setFailureReason(null);
        }
    }

    /**
     * Validates requested shipment status transition.
     *
     * @param current current shipment status
     * @param next next shipment status
     */
    private void validateStatusTransition(ShipmentStatus current, ShipmentStatus next) {
        if (current == next || current == null) {
            return;
        }
        Set<ShipmentStatus> allowed = STATUS_FLOW.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPMENT_STATUS_INVALID_TRANSITION",
                    "Invalid shipment status transition from " + current + " to " + next
            );
        }
    }

    /**
     * Enforces order read access for either admin/internal actors or the order owner.
     *
     * @param context request context
     * @param order order payload
     */
    private void assertOrderAccess(ShippingRequestContext context, OrderServiceClient.OrderPayload order) {
        if (context.canManageAllShipments()) {
            return;
        }
        if (!context.hasSubject()) {
            throw new ShippingOperationException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_SUBJECT_REQUIRED",
                    "Authenticated customer identity is required"
            );
        }
        if (!context.subject().equals(order.customerRef())) {
            throw new ShippingOperationException(
                    HttpStatus.FORBIDDEN,
                    "ORDER_FORBIDDEN",
                    "Order access is forbidden"
            );
        }
    }

    /**
     * Enforces shipment read access for either admin/internal actors or the shipment owner.
     *
     * @param context request context
     * @param shipment shipment aggregate
     */
    private void assertShipmentAccess(ShippingRequestContext context, ShipmentRecord shipment) {
        if (context.canManageAllShipments()) {
            return;
        }
        if (!context.hasSubject()) {
            throw new ShippingOperationException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_SUBJECT_REQUIRED",
                    "Authenticated customer identity is required"
            );
        }
        if (!context.subject().equals(shipment.getCustomerRef())) {
            throw new ShippingOperationException(
                    HttpStatus.FORBIDDEN,
                    "SHIPMENT_FORBIDDEN",
                    "Shipment access is forbidden"
            );
        }
    }

    /**
     * Prevents shipment creation for order states that clearly cannot be fulfilled.
     *
     * @param order order payload
     */
    private void assertOrderSupportsShipment(OrderServiceClient.OrderPayload order) {
        String status = normalizeNullable(order.status());
        if (status == null) {
            return;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(normalized) || "REFUNDED".equals(normalized)) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "ORDER_SHIPMENT_NOT_ALLOWED",
                    "Shipments cannot be created for cancelled or refunded orders"
            );
        }
    }

    /**
     * Returns whether a replacement shipment can be created for one previous shipment.
     *
     * @param existing existing shipment
     * @return {@code true} when replacement shipment creation is allowed
     */
    private boolean canCreateReplacementShipment(ShipmentRecord existing) {
        return existing.getStatus() == ShipmentStatus.CANCELLED || existing.getStatus() == ShipmentStatus.RETURNED;
    }

    /**
     * Maps a web quote request to the carrier quote boundary.
     *
     * @param request web quote request
     * @return carrier quote request
     */
    private ShippingCarrier.QuoteRequest toQuoteRequest(ShippingQuoteRequest request) {
        return new ShippingCarrier.QuoteRequest(
                toCarrierAddress(request.address()),
                normalizeMoney(request.cartSubtotal()),
                normalizeCurrencyCode(request.currencyCode()),
                request.itemCount(),
                normalizeWeight(request.totalWeightKg()),
                normalizeMethodCode(request.methodCode()),
                parseMetadata(request.metadata())
        );
    }

    /**
     * Maps one quote result to the outward response DTO.
     *
     * @param quote carrier quote result
     * @return shipping quote response
     */
    private ShippingQuoteResponse toQuoteResponse(ShippingCarrier.QuoteResult quote) {
        return new ShippingQuoteResponse(
                quote.carrierCode(),
                quote.methodCode(),
                quote.methodName(),
                quote.amount(),
                quote.currencyCode(),
                quote.estimatedDaysMin(),
                quote.estimatedDaysMax(),
                quote.estimatedDeliveryAt(),
                quote.ruleSummary(),
                Instant.now()
        );
    }

    /**
     * Maps a DTO address to the carrier boundary address.
     *
     * @param address source address
     * @return carrier address
     */
    private ShippingCarrier.ShippingAddress toCarrierAddress(AddressRequest address) {
        return new ShippingCarrier.ShippingAddress(
                normalizeNullable(address.fullName()),
                normalizeNullable(address.phone()),
                normalizeNullable(address.line1()),
                normalizeNullable(address.line2()),
                normalizeNullable(address.district()),
                normalizeNullable(address.city()),
                normalizeNullable(address.stateProvince()),
                normalizeNullable(address.postalCode()),
                normalizeCountryCode(address.countryCode())
        );
    }

    /**
     * Maps a parcel DTO to the carrier boundary parcel.
     *
     * @param parcel source parcel
     * @return carrier parcel
     */
    private ShippingCarrier.Parcel toCarrierParcel(ParcelRequest parcel) {
        return new ShippingCarrier.Parcel(
                parcel.quantity(),
                normalizeWeight(parcel.weightKg()),
                normalizeDimension(parcel.lengthCm()),
                normalizeDimension(parcel.widthCm()),
                normalizeDimension(parcel.heightCm())
        );
    }

    /**
     * Resolves a structured recipient address from an order snapshot.
     *
     * @param shippingAddress order shipping address JSON
     * @return structured recipient address
     */
    private AddressRequest resolveRecipientAddress(JsonNode shippingAddress) {
        if (shippingAddress == null || shippingAddress.isNull()) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "ORDER_SHIPPING_ADDRESS_REQUIRED",
                    "Order does not contain a structured shipping address"
            );
        }
        if (!shippingAddress.isObject()) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "ORDER_SHIPPING_ADDRESS_INVALID",
                    "Order shipping address is not structured enough for shipment creation"
            );
        }

        String countryCode = textValue(shippingAddress, "countryCode");
        if (countryCode == null) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "ORDER_SHIPPING_COUNTRY_REQUIRED",
                    "Order shipping address must include countryCode"
            );
        }
        return new AddressRequest(
                firstTextValue(shippingAddress, "fullName", "name", "recipientName"),
                firstTextValue(shippingAddress, "phone", "phoneNumber"),
                firstTextValue(shippingAddress, "line1", "addressLine1"),
                firstTextValue(shippingAddress, "line2", "addressLine2"),
                textValue(shippingAddress, "district"),
                textValue(shippingAddress, "city"),
                firstTextValue(shippingAddress, "stateProvince", "state"),
                textValue(shippingAddress, "postalCode"),
                countryCode
        );
    }

    /**
     * Extracts one JSON text field.
     *
     * @param node source JSON object
     * @param field field name
     * @return normalized field text or {@code null}
     */
    private String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return normalizeNullable(value.asText());
        }
        return normalizeNullable(value.toString());
    }

    /**
     * Extracts the first populated text field from one JSON object.
     *
     * @param node source JSON object
     * @param fields candidate field names
     * @return first non-blank field value or {@code null}
     */
    private String firstTextValue(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = textValue(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Parses metadata into a stable mutable map.
     *
     * @param metadata source metadata
     * @return copied metadata map
     */
    private Map<String, Object> parseMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(metadata);
    }

    /**
     * Reads stored metadata JSON into a map.
     *
     * @param metadataJson stored metadata JSON
     * @return parsed metadata map
     */
    private Map<String, Object> readMetadata(String metadataJson) {
        String normalized = normalizeNullable(metadataJson);
        if (normalized == null) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>(objectMapper.readValue(normalized, MAP_TYPE));
        } catch (JsonProcessingException ex) {
            throw new ShippingOperationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SHIPMENT_METADATA_DESERIALIZATION_FAILED",
                    "Failed to deserialize shipment metadata"
            );
        }
    }

    /**
     * Serializes arbitrary objects to JSON text.
     *
     * @param value source value
     * @return serialized JSON or {@code null}
     */
    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ShippingOperationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SHIPMENT_JSON_SERIALIZATION_FAILED",
                    "Failed to serialize shipment payload"
            );
        }
    }

    /**
     * Maps one shipment aggregate to the outward response DTO.
     *
     * @param shipment source shipment aggregate
     * @return shipment response
     */
    private ShipmentResponse toShipmentResponse(ShipmentRecord shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getOrderNumber(),
                shipment.getCustomerRef(),
                shipment.getShipmentReference(),
                shipment.getCarrierCode(),
                shipment.getMethodCode(),
                shipment.getMethodName(),
                shipment.getStatus(),
                resolveFulfillmentHook(shipment.getStatus()),
                normalizeMoney(shipment.getQuotedAmount()),
                shipment.getCurrencyCode(),
                shipment.getExternalShipmentId(),
                shipment.getTrackingNumber(),
                shipment.getTrackingUrl(),
                shipment.getEstimatedDeliveryAt(),
                shipment.getLabelCreatedAt(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt(),
                shipment.getLastStatusUpdateAt(),
                shipment.getLastCarrierSyncAt(),
                shipment.getFailureReason(),
                readAddress(shipment.getRecipientAddressJson()),
                readParcels(shipment.getParcelSummaryJson()),
                readMetadata(shipment.getMetadataJson()),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }

    /**
     * Reads stored address JSON into the outward address DTO.
     *
     * @param addressJson stored address JSON
     * @return address DTO
     */
    private AddressRequest readAddress(String addressJson) {
        try {
            return objectMapper.readValue(addressJson, AddressRequest.class);
        } catch (JsonProcessingException ex) {
            throw new ShippingOperationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SHIPMENT_ADDRESS_DESERIALIZATION_FAILED",
                    "Failed to deserialize shipment address"
            );
        }
    }

    /**
     * Reads stored parcel JSON into the outward parcel list.
     *
     * @param parcelsJson stored parcels JSON
     * @return parcel list
     */
    private List<ParcelRequest> readParcels(String parcelsJson) {
        try {
            return objectMapper.readValue(parcelsJson, PARCEL_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new ShippingOperationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SHIPMENT_PARCELS_DESERIALIZATION_FAILED",
                    "Failed to deserialize shipment parcels"
            );
        }
    }

    /**
     * Resolves downstream fulfillment hook hints from shipment status.
     *
     * @param status shipment status
     * @return downstream fulfillment hook
     */
    private FulfillmentHookType resolveFulfillmentHook(ShipmentStatus status) {
        if (status == null) {
            return FulfillmentHookType.NONE;
        }
        return switch (status) {
            case CREATED, LABEL_CREATED, READY_FOR_FULFILLMENT -> FulfillmentHookType.ORDER_PACKED;
            case IN_TRANSIT, OUT_FOR_DELIVERY -> FulfillmentHookType.ORDER_SHIPPED;
            case DELIVERED -> FulfillmentHookType.ORDER_DELIVERED;
            case EXCEPTION -> FulfillmentHookType.SHIPMENT_EXCEPTION;
            case CANCELLED -> FulfillmentHookType.SHIPMENT_CANCELLED;
            case RETURNED -> FulfillmentHookType.SHIPMENT_RETURNED;
        };
    }

    /**
     * Generates one business shipment reference.
     *
     * @return shipment reference
     */
    private String generateShipmentReference() {
        return "SHP-" + SHIPMENT_REFERENCE_DATE.format(Instant.now()) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    /**
     * Sums shipment parcel quantities.
     *
     * @param parcels parcel list
     * @return total item count
     */
    private int totalItemCount(List<ParcelRequest> parcels) {
        return parcels.stream()
                .map(ParcelRequest::quantity)
                .reduce(0, Integer::sum);
    }

    /**
     * Sums shipment parcel weights multiplied by quantity.
     *
     * @param parcels parcel list
     * @return total weight
     */
    private BigDecimal totalWeight(List<ParcelRequest> parcels) {
        return parcels.stream()
                .map(parcel -> normalizeWeight(parcel.weightKg()).multiply(BigDecimal.valueOf(parcel.quantity())))
                .reduce(ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Returns whether the current shipment status is terminal.
     *
     * @param status shipment status
     * @return {@code true} when terminal
     */
    private boolean isTerminal(ShipmentStatus status) {
        return status == ShipmentStatus.DELIVERED
                || status == ShipmentStatus.RETURNED
                || status == ShipmentStatus.CANCELLED;
    }

    /**
     * Reads the current correlation ID from MDC.
     *
     * @return correlation ID or {@code null}
     */
    private String currentCorrelationId() {
        return MDC.get("correlationId");
    }

    /**
     * Normalizes monetary values to scale 4 and enforces non-negative numbers.
     *
     * @param value source value
     * @return normalized money
     */
    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_AMOUNT_REQUIRED",
                    "Required monetary values must be provided"
            );
        }
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_AMOUNT_NEGATIVE",
                    "Monetary values must be non-negative"
            );
        }
        return normalized;
    }

    /**
     * Normalizes parcel weight.
     *
     * @param value source weight
     * @return normalized weight
     */
    private BigDecimal normalizeWeight(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_WEIGHT_NEGATIVE",
                    "Weight values must be non-negative"
            );
        }
        return normalized;
    }

    /**
     * Normalizes parcel dimensions.
     *
     * @param value source dimension
     * @return normalized dimension
     */
    private BigDecimal normalizeDimension(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_DIMENSION_NEGATIVE",
                    "Dimension values must be non-negative"
            );
        }
        return normalized;
    }

    /**
     * Normalizes currency code to upper-case.
     *
     * @param value source currency code
     * @return normalized currency code
     */
    private String normalizeCurrencyCode(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_CURRENCY_REQUIRED",
                    "currencyCode is required"
            );
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes country code to upper-case.
     *
     * @param value source country code
     * @return normalized country code
     */
    private String normalizeCountryCode(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_COUNTRY_REQUIRED",
                    "countryCode is required"
            );
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes shipping method code to lowercase.
     *
     * @param value source method code
     * @return normalized method code
     */
    private String normalizeMethodCode(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_METHOD_REQUIRED",
                    "methodCode is required"
            );
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Trims input and returns null for blank strings.
     *
     * @param value source text
     * @return normalized text or null
     */
    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
