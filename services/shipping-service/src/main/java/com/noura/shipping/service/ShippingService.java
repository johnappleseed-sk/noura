package com.noura.shipping.service;

import com.noura.shipping.dto.shipping.CreateShipmentRequest;
import com.noura.shipping.dto.shipping.ShipmentResponse;
import com.noura.shipping.dto.shipping.ShipmentStatusUpdateRequest;
import com.noura.shipping.dto.shipping.ShippingMethodQueryRequest;
import com.noura.shipping.dto.shipping.ShippingMethodResponse;
import com.noura.shipping.dto.shipping.ShippingQuoteRequest;
import com.noura.shipping.dto.shipping.ShippingQuoteResponse;
import com.noura.shipping.service.model.ShippingRequestContext;

import java.util.List;
import java.util.UUID;

/**
 * Application service for shipping method discovery, quote calculation, and shipment lifecycle.
 */
public interface ShippingService {

    /**
     * Resolves available shipping methods for one destination/cart snapshot.
     *
     * @param context request context
     * @param request method discovery request
     * @return available methods
     */
    List<ShippingMethodResponse> getAvailableMethods(ShippingRequestContext context, ShippingMethodQueryRequest request);

    /**
     * Calculates one shipping quote for a selected method.
     *
     * @param context request context
     * @param request quote request
     * @return quote response
     */
    ShippingQuoteResponse quote(ShippingRequestContext context, ShippingQuoteRequest request);

    /**
     * Creates one shipment record for an order.
     *
     * @param context request context
     * @param request shipment creation request
     * @return created shipment response
     */
    ShipmentResponse createShipment(ShippingRequestContext context, CreateShipmentRequest request);

    /**
     * Retrieves one shipment by identifier.
     *
     * @param context request context
     * @param shipmentId shipment identifier
     * @param refreshCarrier whether to refresh carrier status before responding
     * @return shipment response
     */
    ShipmentResponse getShipmentById(ShippingRequestContext context, UUID shipmentId, boolean refreshCarrier);

    /**
     * Retrieves the latest shipment for one order.
     *
     * @param context request context
     * @param orderId order identifier
     * @param refreshCarrier whether to refresh carrier status before responding
     * @return shipment response
     */
    ShipmentResponse getLatestShipmentByOrderId(ShippingRequestContext context, UUID orderId, boolean refreshCarrier);

    /**
     * Applies one trusted internal shipment status update.
     *
     * @param request internal status update request
     * @param actor actor identifier
     * @return updated shipment response
     */
    ShipmentResponse updateShipmentStatus(ShipmentStatusUpdateRequest request, String actor);
}
