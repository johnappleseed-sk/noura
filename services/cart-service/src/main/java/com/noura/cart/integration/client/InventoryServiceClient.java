package com.noura.cart.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.cart.exception.CartOperationException;
import com.noura.cart.integration.InventoryGateway;
import com.noura.cart.integration.model.InventorySnapshot;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST adapter to inventory-service APIs.
 */
@Slf4j
@Component
public class InventoryServiceClient implements InventoryGateway {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private static final ParameterizedTypeReference<RemoteApiEnvelope<List<InventoryStockPayload>>> INVENTORY_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /**
     * Creates inventory REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl inventory service base URL
     */
    public InventoryServiceClient(
            RestClient.Builder builder,
            @Value("${services.inventory.base-url:http://localhost:8086}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InventorySnapshot resolveAvailability(UUID productId, UUID storeId) {
        try {
            RemoteApiEnvelope<List<InventoryStockPayload>> envelope = restClient.get()
                    .uri("/api/inventory/v1/stock-levels/products/{productId}", productId)
                    .retrieve()
                    .body(INVENTORY_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                return new InventorySnapshot(productId, storeId, ZERO, false);
            }
            List<InventoryStockPayload> rows = envelope.data();
            if (rows.isEmpty()) {
                return new InventorySnapshot(productId, storeId, ZERO, false);
            }
            BigDecimal available = resolveAvailableQuantity(rows, storeId);
            return new InventorySnapshot(productId, storeId, available, true);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return new InventorySnapshot(productId, storeId, ZERO, false);
            }
            log.warn("Inventory service call failed for product {}: status={} body={}",
                    productId, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CartOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "INVENTORY_SERVICE_ERROR",
                    "Inventory availability is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Inventory service is unreachable for product {}: {}", productId, ex.getMessage());
            throw new CartOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "INVENTORY_SERVICE_UNREACHABLE",
                    "Inventory availability is temporarily unavailable"
            );
        }
    }

    /**
     * Resolves available quantity from inventory rows.
     *
     * @param rows inventory rows
     * @param storeId optional store/location filter
     * @return normalized available quantity
     */
    private BigDecimal resolveAvailableQuantity(List<InventoryStockPayload> rows, UUID storeId) {
        if (storeId != null) {
            Optional<InventoryStockPayload> exact = rows.stream()
                    .filter(row -> storeId.equals(row.warehouseId()))
                    .findFirst();
            return normalize(exact.map(InventoryStockPayload::quantityAvailable).orElse(BigDecimal.ZERO));
        }
        return rows.stream()
                .map(InventoryStockPayload::quantityAvailable)
                .map(this::normalize)
                .reduce(ZERO, BigDecimal::add);
    }

    /**
     * Normalizes nullable quantity values to scale 4.
     *
     * @param value input value
     * @return normalized quantity
     */
    private BigDecimal normalize(BigDecimal value) {
        return value == null ? ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Generic remote API envelope.
     *
     * @param success success flag
     * @param message response message
     * @param data response payload
     * @param error error payload
     * @param <T> payload type
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteApiEnvelope<T>(
            Boolean success,
            String message,
            T data,
            RemoteError error
    ) {
    }

    /**
     * Remote API error model.
     *
     * @param code stable code
     * @param detail detail message
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteError(String code, String detail) {
    }

    /**
     * Inventory payload model used by cart availability checks.
     *
     * @param warehouseId warehouse/location identifier
     * @param quantityAvailable available quantity
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record InventoryStockPayload(
            UUID warehouseId,
            BigDecimal quantityAvailable
    ) {
    }
}
