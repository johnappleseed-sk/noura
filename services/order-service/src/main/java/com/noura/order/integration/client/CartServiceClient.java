package com.noura.order.integration.client;

import com.noura.order.exception.OrderOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/**
 * Minimal REST adapter used by quick-reorder to rebuild the active cart.
 */
@Slf4j
@Component
public class CartServiceClient {

    private static final String HEADER_SUBJECT = "X-Auth-Subject";
    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private final RestClient restClient;

    /**
     * Creates cart-service client.
     *
     * @param builder rest client builder
     * @param baseUrl cart-service base URL
     */
    public CartServiceClient(
            RestClient.Builder builder,
            @Value("${services.cart.base-url:http://localhost:8088}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Clears the active cart for the provided customer subject.
     *
     * @param customerRef customer subject
     * @param authorizationHeader optional authorization header
     * @param correlationId optional correlation ID
     */
    public void clearCart(String customerRef, String authorizationHeader, String correlationId) {
        exchangeVoid("/api/v1/cart", customerRef, authorizationHeader, correlationId, true, null);
    }

    /**
     * Adds one line item to the active cart for the provided customer subject.
     *
     * @param customerRef customer subject
     * @param authorizationHeader optional authorization header
     * @param correlationId optional correlation ID
     * @param payload add-item payload
     */
    public void addItem(String customerRef, String authorizationHeader, String correlationId, AddCartItemPayload payload) {
        exchangeVoid("/api/v1/cart/items", customerRef, authorizationHeader, correlationId, false, payload);
    }

    private void exchangeVoid(
            String path,
            String customerRef,
            String authorizationHeader,
            String correlationId,
            boolean delete,
            Object payload
    ) {
        try {
            RestClient.RequestBodyUriSpec requestSpec = delete ? null : restClient.post();
            if (delete) {
                restClient.delete()
                        .uri(path)
                        .headers(headers -> applyHeaders(headers, customerRef, authorizationHeader, correlationId))
                        .retrieve()
                        .toBodilessEntity();
                return;
            }
            requestSpec.uri(path)
                    .headers(headers -> applyHeaders(headers, customerRef, authorizationHeader, correlationId))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Cart-service call failed: path={} status={} body={}", path, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new OrderOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CART_SERVICE_ERROR",
                    "Cart service is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Cart-service unreachable: path={} error={}", path, ex.getMessage());
            throw new OrderOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CART_SERVICE_UNREACHABLE",
                    "Cart service is temporarily unavailable"
            );
        }
    }

    private void applyHeaders(
            HttpHeaders headers,
            String customerRef,
            String authorizationHeader,
            String correlationId
    ) {
        headers.set(HEADER_SUBJECT, customerRef);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        if (correlationId != null && !correlationId.isBlank()) {
            headers.set(HEADER_CORRELATION, correlationId);
        }
    }

    /**
     * Minimal cart add-item payload for quick-reorder rebuilds.
     *
     * @param productId product identifier
     * @param variantId optional variant identifier
     * @param quantity quantity
     * @param storeId optional store identifier
     * @param analyticsListName optional analytics list name
     * @param analyticsSlot optional analytics slot
     * @param analyticsPagePath optional analytics page path
     */
    public record AddCartItemPayload(
            UUID productId,
            UUID variantId,
            int quantity,
            UUID storeId,
            String analyticsListName,
            Integer analyticsSlot,
            String analyticsPagePath
    ) {
    }
}
