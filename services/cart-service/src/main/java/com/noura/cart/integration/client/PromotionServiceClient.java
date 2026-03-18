package com.noura.cart.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.cart.exception.CartOperationException;
import com.noura.cart.integration.PromotionGateway;
import com.noura.cart.integration.model.PromotionEvaluationItem;
import com.noura.cart.integration.model.PromotionEvaluationSnapshot;
import com.noura.cart.integration.model.PromotionValidationSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter to promotion-service coupon validation APIs.
 */
@Slf4j
@Component
public class PromotionServiceClient implements PromotionGateway {

    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private static final ParameterizedTypeReference<RemoteApiEnvelope<PromotionValidationPayload>> VALIDATION_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /**
     * Creates promotion-service REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl promotion-service base URL
     */
    public PromotionServiceClient(
            RestClient.Builder builder,
            @Value("${services.promotion.base-url:http://localhost:8094}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Validates one coupon/promo code against the current cart snapshot.
     *
     * @param couponCode coupon code value
     * @param subtotal cart subtotal
     * @param items cart line snapshots used for eligibility checks
     * @param correlationId request correlation ID
     * @return promotion validation snapshot
     */
    @Override
    public PromotionValidationSnapshot validateCoupon(
            String couponCode,
            BigDecimal subtotal,
            List<PromotionEvaluationItem> items,
            String correlationId
    ) {
        try {
            RemoteApiEnvelope<PromotionValidationPayload> envelope = restClient.post()
                    .uri("/api/v1/promotions/validate-code")
                    .headers(headers -> {
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.set(HEADER_CORRELATION, correlationId);
                        }
                    })
                    .body(new PromotionValidationRequestPayload(
                            couponCode,
                            subtotal,
                            null,
                            mapItems(items)
                    ))
                    .retrieve()
                    .body(VALIDATION_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new CartOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "PROMOTION_SERVICE_INVALID_RESPONSE",
                        "Promotion service returned an invalid coupon-validation response"
                );
            }
            return toSnapshot(envelope.data());
        } catch (RestClientResponseException ex) {
            log.warn("Promotion service coupon validation failed: status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CartOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PROMOTION_SERVICE_ERROR",
                    "Promotion validation is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Promotion service coupon validation unreachable: {}", ex.getMessage());
            throw new CartOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PROMOTION_SERVICE_UNREACHABLE",
                    "Promotion validation is temporarily unavailable"
            );
        }
    }

    /**
     * Maps local cart-line snapshots into promotion-service evaluation-item payloads.
     *
     * @param items local cart-line inputs
     * @return mapped promotion-service item payloads
     */
    private List<PromotionEvaluationItemPayload> mapItems(List<PromotionEvaluationItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(item -> new PromotionEvaluationItemPayload(
                        item.productId(),
                        null,
                        item.variantId(),
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();
    }

    /**
     * Converts remote payload into cart-service integration snapshot.
     *
     * @param payload remote validation payload
     * @return normalized snapshot
     */
    private PromotionValidationSnapshot toSnapshot(PromotionValidationPayload payload) {
        PromotionEvaluationSnapshot evaluation = payload.evaluation() == null
                ? null
                : new PromotionEvaluationSnapshot(
                payload.evaluation().discountAmount(),
                payload.evaluation().freeShipping()
        );
        return new PromotionValidationSnapshot(
                payload.valid(),
                payload.eligible(),
                payload.reasonCode(),
                payload.reasonMessage(),
                evaluation
        );
    }

    /**
     * Generic remote API envelope.
     *
     * @param success success flag
     * @param message response message
     * @param data response payload
     * @param error remote error payload
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
     * Promotion validation request payload.
     *
     * @param promoCode promo/coupon code
     * @param subtotal cart subtotal
     * @param customerSegment optional customer segment (unused in cart-service)
     * @param items evaluation items
     */
    private record PromotionValidationRequestPayload(
            String promoCode,
            BigDecimal subtotal,
            String customerSegment,
            List<PromotionEvaluationItemPayload> items
    ) {
    }

    /**
     * Promotion-evaluation item payload accepted by promotion-service.
     *
     * @param productId product identifier
     * @param categoryId optional category identifier
     * @param variantId optional variant identifier
     * @param quantity quantity
     * @param unitPrice unit price
     */
    private record PromotionEvaluationItemPayload(
            UUID productId,
            UUID categoryId,
            UUID variantId,
            int quantity,
            BigDecimal unitPrice
    ) {
    }

    /**
     * Promotion validation response payload.
     *
     * @param valid whether promo code exists
     * @param eligible whether input cart qualifies
     * @param reasonCode machine-readable reason code
     * @param reasonMessage human-readable reason message
     * @param evaluation discount evaluation result
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PromotionValidationPayload(
            boolean valid,
            boolean eligible,
            String reasonCode,
            String reasonMessage,
            PromotionEvaluationPayload evaluation
    ) {
    }

    /**
     * Promotion discount evaluation payload.
     *
     * @param discountAmount total discount amount
     * @param freeShipping whether the promotion grants free shipping
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PromotionEvaluationPayload(
            BigDecimal discountAmount,
            boolean freeShipping
    ) {
    }
}
