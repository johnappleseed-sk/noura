package com.noura.cart.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.cart.exception.CartOperationException;
import com.noura.cart.integration.CatalogGateway;
import com.noura.cart.integration.model.ProductSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST adapter to catalog-service product APIs.
 */
@Slf4j
@Component
public class CatalogServiceClient implements CatalogGateway {

    private static final ParameterizedTypeReference<RemoteApiEnvelope<CatalogProductPayload>> PRODUCT_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /**
     * Creates catalog REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl catalog service base URL
     */
    public CatalogServiceClient(
            RestClient.Builder builder,
            @Value("${services.catalog.base-url:http://localhost:8084}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ProductSnapshot> findProduct(UUID productId) {
        try {
            RemoteApiEnvelope<CatalogProductPayload> envelope = restClient.get()
                    .uri("/api/v1/products/{productId}", productId)
                    .retrieve()
                    .body(PRODUCT_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                return Optional.empty();
            }
            CatalogProductPayload payload = envelope.data();
            String representativeSku = payload.variants() == null || payload.variants().isEmpty()
                    ? null
                    : payload.variants().getFirst().sku();
            return Optional.of(new ProductSnapshot(
                    payload.id(),
                    payload.name(),
                    null,
                    representativeSku,
                    payload.allowBackorder()
            ));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            log.warn("Catalog service call failed for product {}: status={} body={}",
                    productId, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CartOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CATALOG_SERVICE_ERROR",
                    "Catalog validation is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Catalog service is unreachable for product {}: {}", productId, ex.getMessage());
            throw new CartOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CATALOG_SERVICE_UNREACHABLE",
                    "Catalog validation is temporarily unavailable"
            );
        }
    }

    /**
     * Generic remote API envelope.
     *
     * @param success success flag
     * @param message response message
     * @param data response payload
     * @param error error details
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
     * @param code stable error code
     * @param detail detail message
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteError(String code, String detail) {
    }

    /**
     * Catalog product payload model used by the cart adapter.
     *
     * @param id product identifier
     * @param name product name
     * @param allowBackorder backorder flag
     * @param variants variant list
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogProductPayload(
            UUID id,
            String name,
            boolean allowBackorder,
            List<CatalogVariantPayload> variants
    ) {
    }

    /**
     * Catalog variant payload model used to capture a representative SKU.
     *
     * @param sku variant SKU
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogVariantPayload(String sku) {
    }
}
