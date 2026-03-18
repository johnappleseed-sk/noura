package com.noura.checkout.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.checkout.exception.CheckoutOperationException;
import com.noura.checkout.integration.model.RemoteApiEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * REST adapter to inventory-service APIs.
 */
@Slf4j
@Component
public class InventoryServiceClient {

    private static final String HEADER_CORRELATION = "X-Correlation-ID";
    private static final String HEADER_SUBJECT = "X-Auth-Subject";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private static final ParameterizedTypeReference<RemoteApiEnvelope<StockLevelPayload>> STOCK_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<RemoteApiEnvelope<StockOperationPayload>> STOCK_OPERATION_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /**
     * Creates inventory-service REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl inventory-service base URL
     */
    public InventoryServiceClient(
            RestClient.Builder builder,
            @Value("${services.inventory.base-url:http://localhost:8086}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Resolves available stock for one product at one location.
     *
     * @param productId product identifier
     * @param locationId location identifier
     * @param correlationId correlation ID for tracing
     * @return available quantity
     */
    public BigDecimal resolveAvailable(UUID productId, UUID locationId, String correlationId) {
        try {
            RemoteApiEnvelope<StockLevelPayload> envelope = restClient.get()
                    .uri("/api/inventory/v1/stock-levels/products/{productId}/locations/{locationId}", productId, locationId)
                    .header(HEADER_CORRELATION, correlationId)
                    .retrieve()
                    .body(STOCK_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                return ZERO;
            }
            return normalize(envelope.data().quantityAvailable());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ZERO;
            }
            log.warn("Inventory stock lookup failed for product {}: status={} body={}",
                    productId, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "INVENTORY_SERVICE_ERROR",
                    "Inventory availability is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Inventory service unreachable for product {}: {}", productId, ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "INVENTORY_SERVICE_UNREACHABLE",
                    "Inventory availability is temporarily unavailable"
            );
        }
    }

    /**
     * Reserves stock for one product/location pair.
     *
     * @param productId product identifier
     * @param locationId location identifier
     * @param quantity quantity to reserve
     * @param actor customer/actor identifier
     * @param referenceId checkout reference identifier
     * @param correlationId correlation ID for tracing
     * @return reservation operation result
     */
    public ReservationResult reserve(
            UUID productId,
            UUID locationId,
            BigDecimal quantity,
            String actor,
            String referenceId,
            String correlationId
    ) {
        return mutateReservation(
                "/api/inventory/v1/stock-levels/reservations",
                productId,
                locationId,
                quantity,
                actor,
                referenceId,
                correlationId
        );
    }

    /**
     * Releases a previously reserved stock amount.
     *
     * @param productId product identifier
     * @param locationId location identifier
     * @param quantity quantity to release
     * @param actor customer/actor identifier
     * @param referenceId checkout reference identifier
     * @param correlationId correlation ID for tracing
     */
    public void release(
            UUID productId,
            UUID locationId,
            BigDecimal quantity,
            String actor,
            String referenceId,
            String correlationId
    ) {
        mutateReservation(
                "/api/inventory/v1/stock-levels/reservations/release",
                productId,
                locationId,
                quantity,
                actor,
                referenceId,
                correlationId
        );
    }

    /**
     * Executes a reserve/release mutation endpoint and returns operation metadata.
     *
     * @param path target mutation path
     * @param productId product identifier
     * @param locationId location identifier
     * @param quantity quantity
     * @param actor actor identifier
     * @param referenceId reference ID
     * @param correlationId correlation ID
     * @return operation result
     */
    private ReservationResult mutateReservation(
            String path,
            UUID productId,
            UUID locationId,
            BigDecimal quantity,
            String actor,
            String referenceId,
            String correlationId
    ) {
        try {
            RemoteApiEnvelope<StockOperationPayload> envelope = restClient.post()
                    .uri(path)
                    .header(HEADER_CORRELATION, correlationId)
                    .header(HEADER_SUBJECT, actor)
                    .body(new ReservationCommandPayload(
                            productId,
                            locationId,
                            normalize(quantity),
                            "CHECKOUT_PLACE_ORDER",
                            "CHECKOUT",
                            referenceId,
                            null
                    ))
                    .retrieve()
                    .body(STOCK_OPERATION_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new CheckoutOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "INVENTORY_SERVICE_INVALID_RESPONSE",
                        "Inventory service returned an invalid reservation response"
                );
            }
            return new ReservationResult(
                    envelope.data().movementId(),
                    productId,
                    locationId,
                    normalize(quantity)
            );
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new CheckoutOperationException(
                        HttpStatus.CONFLICT,
                        "INSUFFICIENT_STOCK",
                        "Insufficient stock for one or more checkout lines"
                );
            }
            log.warn("Inventory reservation mutation failed: path={} status={} body={}",
                    path, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "INVENTORY_MUTATION_ERROR",
                    "Inventory mutation is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Inventory reservation mutation unreachable: path={} error={}", path, ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "INVENTORY_MUTATION_UNREACHABLE",
                    "Inventory mutation is temporarily unavailable"
            );
        }
    }

    /**
     * Normalizes decimal values to scale 4.
     *
     * @param value input value
     * @return normalized value
     */
    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Reservation operation result.
     *
     * @param movementId inventory movement identifier
     * @param productId product identifier
     * @param locationId location identifier
     * @param quantity reserved quantity
     */
    public record ReservationResult(
            UUID movementId,
            UUID productId,
            UUID locationId,
            BigDecimal quantity
    ) {
    }

    /**
     * Stock level payload returned by inventory-service.
     *
     * @param quantityAvailable available quantity
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StockLevelPayload(
            BigDecimal quantityAvailable
    ) {
    }

    /**
     * Reservation/release command payload accepted by inventory-service.
     *
     * @param productId product identifier
     * @param locationId location identifier
     * @param quantity quantity
     * @param reasonCode reason code
     * @param referenceType reference type
     * @param referenceId reference ID
     * @param notes optional notes
     */
    private record ReservationCommandPayload(
            UUID productId,
            UUID locationId,
            BigDecimal quantity,
            String reasonCode,
            String referenceType,
            String referenceId,
            String notes
    ) {
    }

    /**
     * Stock operation payload returned by inventory-service.
     *
     * @param movementId inventory movement ID
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StockOperationPayload(
            UUID movementId
    ) {
    }
}
