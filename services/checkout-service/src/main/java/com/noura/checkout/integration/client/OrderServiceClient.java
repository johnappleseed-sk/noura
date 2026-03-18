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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST adapter to order-service APIs.
 */
@Slf4j
@Component
public class OrderServiceClient {

    private static final String HEADER_SUBJECT = "X-Auth-Subject";
    private static final String HEADER_CORRELATION = "X-Correlation-ID";
    private static final String HEADER_INTERNAL_API_KEY = "X-Internal-Api-Key";

    private static final ParameterizedTypeReference<RemoteApiEnvelope<OrderPayload>> ORDER_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String internalApiKey;

    /**
     * Creates order-service REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl order-service base URL
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
     * Creates one order using order-service deterministic idempotency flow.
     *
     * @param customerRef resolved customer subject
     * @param authorizationHeader optional authorization header
     * @param correlationId correlation ID for tracing
     * @param request create-order request payload
     * @return created/existing order payload
     */
    public OrderPayload createOrder(
            String customerRef,
            String authorizationHeader,
            String correlationId,
            CreateOrderPayload request
    ) {
        try {
            RemoteApiEnvelope<OrderPayload> envelope = restClient.post()
                    .uri("/api/v1/orders")
                    .headers(headers -> {
                        headers.set(HEADER_SUBJECT, customerRef);
                        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
                        }
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.set(HEADER_CORRELATION, correlationId);
                        }
                    })
                    .body(request)
                    .retrieve()
                    .body(ORDER_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new CheckoutOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "ORDER_SERVICE_INVALID_RESPONSE",
                        "Order service returned an invalid create-order response"
                );
            }
            return envelope.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new CheckoutOperationException(
                        HttpStatus.CONFLICT,
                        "ORDER_CREATE_CONFLICT",
                        "Order creation is currently conflicted and cannot proceed"
                );
            }
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new CheckoutOperationException(
                        HttpStatus.BAD_REQUEST,
                        "ORDER_CREATE_INVALID_REQUEST",
                        "Order service rejected checkout payload"
                );
            }
            log.warn("Order service create-order failed: status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "ORDER_SERVICE_ERROR",
                    "Order creation is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Order service unreachable: {}", ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "ORDER_SERVICE_UNREACHABLE",
                    "Order creation is temporarily unavailable"
            );
        }
    }

    /**
     * Loads one order by ID.
     *
     * @param customerRef resolved customer subject
     * @param authorizationHeader optional authorization header
     * @param correlationId correlation ID for tracing
     * @param orderId order identifier
     * @return order payload
     */
    public OrderPayload getOrderById(
            String customerRef,
            String authorizationHeader,
            String correlationId,
            UUID orderId
    ) {
        try {
            RemoteApiEnvelope<OrderPayload> envelope = restClient.get()
                    .uri("/api/v1/orders/{orderId}", orderId)
                    .headers(headers -> {
                        headers.set(HEADER_SUBJECT, customerRef);
                        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
                        }
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.set(HEADER_CORRELATION, correlationId);
                        }
                    })
                    .retrieve()
                    .body(ORDER_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new CheckoutOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "ORDER_SERVICE_INVALID_RESPONSE",
                        "Order service returned an invalid get-order response"
                );
            }
            return envelope.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CheckoutOperationException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found");
            }
            log.warn("Order service get-order failed: status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "ORDER_SERVICE_ERROR",
                    "Order retrieval is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Order service get-order unreachable: {}", ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "ORDER_SERVICE_UNREACHABLE",
                    "Order retrieval is temporarily unavailable"
            );
        }
    }

    /**
     * Updates one order status through the trusted internal order lifecycle API.
     *
     * @param orderId order identifier
     * @param correlationId correlation ID for tracing
     * @param request order status update payload
     * @return updated order payload
     */
    public OrderPayload updateOrderStatusInternal(
            UUID orderId,
            String correlationId,
            UpdateOrderStatusPayload request
    ) {
        try {
            RemoteApiEnvelope<OrderPayload> envelope = restClient.post()
                    .uri("/internal/orders/{orderId}/status", orderId)
                    .headers(headers -> {
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.set(HEADER_CORRELATION, correlationId);
                        }
                        if (internalApiKey != null && !internalApiKey.isBlank()) {
                            headers.set(HEADER_INTERNAL_API_KEY, internalApiKey.trim());
                        }
                    })
                    .body(request)
                    .retrieve()
                    .body(ORDER_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new CheckoutOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "ORDER_SERVICE_INVALID_RESPONSE",
                        "Order service returned an invalid order status update response"
                );
            }
            return envelope.data();
        } catch (RestClientResponseException ex) {
            log.warn("Order service internal status update failed: status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "ORDER_FINALIZATION_FAILED",
                    "Order finalization is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Order service internal status update unreachable: {}", ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "ORDER_SERVICE_UNREACHABLE",
                    "Order finalization is temporarily unavailable"
            );
        }
    }

    /**
     * Order creation payload accepted by order-service.
     *
     * @param customerRef customer reference
     * @param storeId store identifier
     * @param addressId address identifier
     * @param currencyCode currency code
     * @param paymentReference payment reference
     * @param couponCode coupon code
     * @param shippingAddress shipping snapshot object
     * @param billingAddress billing snapshot object
     * @param shippingAddressSnapshot plain shipping snapshot string
     * @param checkoutContext additional checkout snapshot
     * @param subtotal subtotal amount
     * @param discountAmount discount amount
     * @param shippingAmount shipping amount
     * @param taxAmount tax amount
     * @param totalAmount total amount
     * @param paymentConfirmed payment confirmation marker
     * @param idempotencyKey idempotency key
     * @param items immutable line payloads
     */
    public record CreateOrderPayload(
            String customerRef,
            UUID storeId,
            UUID addressId,
            String currencyCode,
            String paymentReference,
            String couponCode,
            AddressSnapshotPayload shippingAddress,
            AddressSnapshotPayload billingAddress,
            String shippingAddressSnapshot,
            Map<String, Object> checkoutContext,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal shippingAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            Boolean paymentConfirmed,
            String idempotencyKey,
            List<CreateOrderItemPayload> items
    ) {
    }

    /**
     * Internal order lifecycle update payload accepted by order-service.
     *
     * @param status target order status
     * @param refundStatus target refund status
     * @param reason stable transition reason
     * @param note operator note
     */
    public record UpdateOrderStatusPayload(
            String status,
            String refundStatus,
            String reason,
            String note
    ) {
    }

    /**
     * Create-order line payload accepted by order-service.
     *
     * @param productId product identifier
     * @param variantId optional variant identifier
     * @param sku sku code
     * @param productName product name snapshot
     * @param variantName variant name snapshot
     * @param quantity quantity
     * @param unitPrice unit price
     * @param lineTotal line total
     * @param itemSnapshot per-line snapshot metadata
     */
    public record CreateOrderItemPayload(
            UUID productId,
            UUID variantId,
            String sku,
            String productName,
            String variantName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            Map<String, Object> itemSnapshot
    ) {
    }

    /**
     * Address snapshot payload accepted by order-service.
     *
     * @param fullName recipient full name
     * @param phone recipient phone
     * @param line1 address line 1
     * @param line2 address line 2
     * @param district district
     * @param city city
     * @param stateProvince state/province
     * @param postalCode postal code
     * @param countryCode country code
     */
    public record AddressSnapshotPayload(
            String fullName,
            String phone,
            String line1,
            String line2,
            String district,
            String city,
            String stateProvince,
            String postalCode,
            String countryCode
    ) {
    }

    /**
     * Order payload returned by order-service.
     *
     * @param id order identifier
     * @param orderNumber business order number
     * @param status order status
     * @param totalAmount total amount
     * @param currencyCode currency code
     * @param placedAt placement timestamp
     * @param createdAt fallback creation timestamp
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderPayload(
            UUID id,
            String orderNumber,
            String status,
            BigDecimal totalAmount,
            String currencyCode,
            Instant placedAt,
            Instant createdAt
    ) {
    }
}
