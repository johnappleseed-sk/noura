package com.noura.shipping.controller;

import com.noura.shipping.common.ApiResponse;
import com.noura.shipping.config.InternalApiProperties;
import com.noura.shipping.controller.support.ShippingRequestContextResolver;
import com.noura.shipping.dto.shipping.CreateShipmentRequest;
import com.noura.shipping.dto.shipping.ShipmentResponse;
import com.noura.shipping.dto.shipping.ShipmentStatusUpdateRequest;
import com.noura.shipping.dto.shipping.ShippingMethodQueryRequest;
import com.noura.shipping.dto.shipping.ShippingMethodResponse;
import com.noura.shipping.dto.shipping.ShippingQuoteRequest;
import com.noura.shipping.dto.shipping.ShippingQuoteResponse;
import com.noura.shipping.exception.ShippingOperationException;
import com.noura.shipping.service.ShippingService;
import com.noura.shipping.service.model.ShippingRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for shipping method lookup, quoting, shipment creation, and shipment status reads.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping
public class ShippingController {

    private final ShippingService shippingService;
    private final ShippingRequestContextResolver contextResolver;
    private final InternalApiProperties internalApiProperties;

    /**
     * Lists available shipping methods for one destination/cart snapshot.
     *
     * @param requestQuery method discovery query parameters
     * @param request current HTTP request
     * @return shipping method list envelope
     */
    @GetMapping({"/api/v1/shipping/methods", "/api/shipping/methods"})
    public ApiResponse<List<ShippingMethodResponse>> getShippingMethods(
            @Valid @ModelAttribute ShippingMethodQueryRequest requestQuery,
            HttpServletRequest request
    ) {
        ShippingRequestContext context = contextResolver.resolve(request);
        List<ShippingMethodResponse> data = shippingService.getAvailableMethods(context, requestQuery);
        return ApiResponse.ok("Shipping methods", data, request.getRequestURI());
    }

    /**
     * Calculates one quote for a selected shipping method.
     *
     * @param requestBody quote request
     * @param request current HTTP request
     * @return quote response envelope
     */
    @PostMapping({"/api/v1/shipping/quotes", "/api/shipping/quotes"})
    public ApiResponse<ShippingQuoteResponse> quoteShipping(
            @Valid @RequestBody ShippingQuoteRequest requestBody,
            HttpServletRequest request
    ) {
        ShippingRequestContext context = contextResolver.resolve(request);
        ShippingQuoteResponse data = shippingService.quote(context, requestBody);
        return ApiResponse.ok("Shipping quote", data, request.getRequestURI());
    }

    /**
     * Creates one shipment for an order.
     *
     * @param requestBody shipment creation request
     * @param request current HTTP request
     * @return created shipment envelope
     */
    @PostMapping({"/api/v1/shipping/shipments", "/api/shipping/shipments"})
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(
            @Valid @RequestBody CreateShipmentRequest requestBody,
            HttpServletRequest request
    ) {
        ShippingRequestContext context = contextResolver.resolve(request);
        ShipmentResponse data = shippingService.createShipment(context, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Shipment created", data, request.getRequestURI()));
    }

    /**
     * Retrieves one shipment by identifier.
     *
     * @param shipmentId shipment identifier
     * @param refreshCarrier whether to refresh carrier status before responding
     * @param request current HTTP request
     * @return shipment response envelope
     */
    @GetMapping({"/api/v1/shipping/shipments/{shipmentId}", "/api/shipping/shipments/{shipmentId}"})
    public ApiResponse<ShipmentResponse> getShipmentById(
            @PathVariable UUID shipmentId,
            @RequestParam(defaultValue = "false") boolean refreshCarrier,
            HttpServletRequest request
    ) {
        ShippingRequestContext context = contextResolver.resolve(request);
        ShipmentResponse data = shippingService.getShipmentById(context, shipmentId, refreshCarrier);
        return ApiResponse.ok("Shipment", data, request.getRequestURI());
    }

    /**
     * Retrieves the latest shipment for one order.
     *
     * @param orderId order identifier
     * @param refreshCarrier whether to refresh carrier status before responding
     * @param request current HTTP request
     * @return shipment response envelope
     */
    @GetMapping({"/api/v1/shipping/shipments/order/{orderId}", "/api/shipping/shipments/order/{orderId}"})
    public ApiResponse<ShipmentResponse> getShipmentByOrderId(
            @PathVariable UUID orderId,
            @RequestParam(defaultValue = "false") boolean refreshCarrier,
            HttpServletRequest request
    ) {
        ShippingRequestContext context = contextResolver.resolve(request);
        ShipmentResponse data = shippingService.getLatestShipmentByOrderId(context, orderId, refreshCarrier);
        return ApiResponse.ok("Shipment", data, request.getRequestURI());
    }

    /**
     * Applies one trusted internal shipment status update.
     *
     * @param requestBody status update payload
     * @param providedApiKey optional internal API key
     * @param request current HTTP request
     * @return updated shipment envelope
     */
    @PostMapping("/internal/shipping/shipments/status-update")
    public ApiResponse<ShipmentResponse> updateShipmentStatus(
            @Valid @RequestBody ShipmentStatusUpdateRequest requestBody,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedApiKey,
            HttpServletRequest request
    ) {
        validateInternalApiKey(providedApiKey);
        ShipmentResponse data = shippingService.updateShipmentStatus(requestBody, "internal");
        return ApiResponse.ok("Shipment status updated", data, request.getRequestURI());
    }

    /**
     * Validates the internal API key when one is configured.
     *
     * @param providedApiKey API key from the request header
     */
    private void validateInternalApiKey(String providedApiKey) {
        String configuredApiKey = trimToNull(internalApiProperties.getApiKey());
        if (configuredApiKey == null) {
            return;
        }
        if (!configuredApiKey.equals(trimToNull(providedApiKey))) {
            throw new ShippingOperationException(
                    HttpStatus.FORBIDDEN,
                    "INTERNAL_API_KEY_INVALID",
                    "Invalid internal API key"
            );
        }
    }

    /**
     * Trims text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
