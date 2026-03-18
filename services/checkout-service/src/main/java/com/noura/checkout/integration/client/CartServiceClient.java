package com.noura.checkout.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.checkout.exception.CheckoutOperationException;
import com.noura.checkout.integration.model.RemoteApiEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter to cart-service APIs.
 */
@Slf4j
@Component
public class CartServiceClient {

    private static final String HEADER_SUBJECT = "X-Auth-Subject";
    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private static final ParameterizedTypeReference<RemoteApiEnvelope<CartPayload>> CART_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /**
     * Creates cart-service REST adapter.
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
     * Loads active cart for the current customer context.
     *
     * @param customerRef resolved customer subject
     * @param authorizationHeader optional authorization header
     * @param correlationId correlation ID for tracing
     * @return cart payload
     */
    public CartPayload getActiveCart(String customerRef, String authorizationHeader, String correlationId) {
        try {
            RemoteApiEnvelope<CartPayload> envelope = restClient.get()
                    .uri("/api/v1/cart")
                    .headers(headers -> applyHeaders(headers, customerRef, authorizationHeader, correlationId))
                    .retrieve()
                    .body(CART_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new CheckoutOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "CART_SERVICE_INVALID_RESPONSE",
                        "Cart service returned an invalid response"
                );
            }
            return envelope.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CheckoutOperationException(HttpStatus.NOT_FOUND, "CART_NOT_FOUND", "Cart not found");
            }
            log.warn("Cart service get-cart failed: status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CART_SERVICE_ERROR",
                    "Cart data is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Cart service unreachable: {}", ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CART_SERVICE_UNREACHABLE",
                    "Cart data is temporarily unavailable"
            );
        }
    }

    /**
     * Clears current customer cart after successful order placement.
     *
     * @param customerRef resolved customer subject
     * @param authorizationHeader optional authorization header
     * @param correlationId correlation ID for tracing
     */
    public void clearCart(String customerRef, String authorizationHeader, String correlationId) {
        try {
            restClient.delete()
                    .uri("/api/v1/cart")
                    .headers(headers -> applyHeaders(headers, customerRef, authorizationHeader, correlationId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Cart clear failed: status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            log.warn("Cart clear skipped because cart-service is unreachable: {}", ex.getMessage());
        }
    }

    /**
     * Applies shared gateway-forwarded headers.
     *
     * @param headers mutable header collection
     * @param customerRef customer subject
     * @param authorizationHeader optional authorization header
     * @param correlationId correlation identifier
     */
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
     * Cart payload returned by cart-service.
     *
     * @param cartId cart identifier
     * @param storeId store/location identifier
     * @param addressId address identifier
     * @param currencyCode currency code
     * @param items cart line items
     * @param totals totals payload
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CartPayload(
            UUID cartId,
            UUID storeId,
            UUID addressId,
            String currencyCode,
            List<CartItemPayload> items,
            CartTotalsPayload totals
    ) {
    }

    /**
     * Cart line payload returned by cart-service.
     *
     * @param id cart item identifier
     * @param productId product identifier
     * @param variantId optional variant identifier
     * @param storeId optional store/location identifier
     * @param productName product name snapshot
     * @param sku sku snapshot
     * @param quantity quantity
     * @param unitPrice unit price snapshot
     * @param lineTotal line total snapshot
     * @param availableQuantity latest available quantity
     * @param validationStatus validation status
     * @param validationMessage validation detail
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CartItemPayload(
            UUID id,
            UUID productId,
            UUID variantId,
            UUID storeId,
            String productName,
            String sku,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            BigDecimal availableQuantity,
            String validationStatus,
            String validationMessage
    ) {
    }

    /**
     * Cart totals payload.
     *
     * @param subtotal subtotal amount
     * @param discountAmount discount amount
     * @param shippingAmount shipping amount
     * @param totalAmount total amount
     * @param couponCode coupon code
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CartTotalsPayload(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal shippingAmount,
            BigDecimal totalAmount,
            String couponCode
    ) {
    }
}
