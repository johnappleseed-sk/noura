package com.noura.shipping.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.noura.shipping.exception.ShippingOperationException;
import com.noura.shipping.integration.model.RemoteApiEnvelope;
import com.noura.shipping.service.model.ShippingRequestContext;
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
import java.util.UUID;

/**
 * REST adapter used by shipping-service to validate and inspect orders.
 */
@Slf4j
@Component
public class OrderServiceClient {

    private static final String HEADER_SUBJECT = "X-Auth-Subject";
    private static final String HEADER_INTERNAL_API_KEY = "X-Internal-Api-Key";
    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private static final ParameterizedTypeReference<RemoteApiEnvelope<OrderPayload>> ORDER_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String internalApiKey;

    /**
     * Creates the order-service REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl order-service base URL
     * @param internalApiKey optional shared internal API key
     */
    public OrderServiceClient(
            RestClient.Builder builder,
            @Value("${services.order.base-url:http://localhost:8090}") String baseUrl,
            @Value("${services.order.internal-api-key:}") String internalApiKey
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    /**
     * Loads one order by ID while forwarding correlation and actor context.
     *
     * @param context request actor context
     * @param correlationId correlation ID for tracing
     * @param orderId order identifier
     * @return order payload
     */
    public OrderPayload getOrderById(
            ShippingRequestContext context,
            String correlationId,
            UUID orderId
    ) {
        try {
            RemoteApiEnvelope<OrderPayload> envelope = restClient.get()
                    .uri("/api/v1/orders/{orderId}", orderId)
                    .headers(headers -> {
                        if (context != null && context.hasSubject()) {
                            headers.set(HEADER_SUBJECT, context.subject());
                        }
                        if (context != null && context.authorizationHeader() != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, context.authorizationHeader());
                        }
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.set(HEADER_CORRELATION, correlationId);
                        }
                        if (internalApiKey != null && !internalApiKey.isBlank()) {
                            headers.set(HEADER_INTERNAL_API_KEY, internalApiKey);
                        }
                    })
                    .retrieve()
                    .body(ORDER_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new ShippingOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "ORDER_SERVICE_INVALID_RESPONSE",
                        "Order service returned an invalid order lookup response"
                );
            }
            return envelope.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ShippingOperationException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found");
            }
            if (ex.getStatusCode() == HttpStatus.FORBIDDEN || ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new ShippingOperationException(
                        HttpStatus.FORBIDDEN,
                        "ORDER_ACCESS_FORBIDDEN",
                        "Order access is forbidden for the current actor"
                );
            }
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new ShippingOperationException(
                        HttpStatus.BAD_REQUEST,
                        "ORDER_LOOKUP_INVALID",
                        "Order service rejected the lookup request"
                );
            }
            log.warn("Order service get-order failed: status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new ShippingOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "ORDER_SERVICE_ERROR",
                    "Order lookup is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Order service get-order unreachable: {}", ex.getMessage());
            throw new ShippingOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "ORDER_SERVICE_UNREACHABLE",
                    "Order lookup is temporarily unavailable"
            );
        }
    }

    /**
     * Minimal order payload required by shipping-service.
     *
     * @param id order identifier
     * @param orderNumber business order number
     * @param customerRef customer reference
     * @param subtotal order subtotal amount
     * @param shippingAmount order shipping amount
     * @param totalAmount order total amount
     * @param currencyCode order currency code
     * @param shippingAddress order shipping address snapshot
     * @param status current order status
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderPayload(
            UUID id,
            String orderNumber,
            String customerRef,
            BigDecimal subtotal,
            BigDecimal shippingAmount,
            BigDecimal totalAmount,
            String currencyCode,
            JsonNode shippingAddress,
            String status
    ) {
    }
}
