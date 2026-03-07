package com.noura.platform.commerce.fulfillment.web;

import com.noura.platform.commerce.api.v1.dto.common.ApiEnvelope;
import com.noura.platform.commerce.api.v1.support.ApiTrace;
import com.noura.platform.commerce.fulfillment.application.StorefrontFulfillmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/fulfillment")
public class AdminFulfillmentController {
    private final StorefrontFulfillmentService fulfillmentService;

    public AdminFulfillmentController(StorefrontFulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping
    public ApiEnvelope<StorefrontFulfillmentService.ShipmentDto> getLatest(@PathVariable Long orderId,
                                                                          HttpServletRequest requestContext) {
        return ApiEnvelope.success(
                "ADMIN_FULFILLMENT_GET_OK",
                "Fulfillment status fetched successfully.",
                fulfillmentService.getLatestForOrderStaff(orderId),
                ApiTrace.resolve(requestContext)
        );
    }

    @PostMapping
    public ApiEnvelope<StorefrontFulfillmentService.ShipmentDto> update(@PathVariable Long orderId,
                                                                        @Valid @RequestBody StorefrontFulfillmentService.UpsertShipmentRequest body,
                                                                        HttpServletRequest requestContext) {
        return ApiEnvelope.success(
                "ADMIN_FULFILLMENT_UPDATE_OK",
                "Fulfillment updated successfully.",
                fulfillmentService.updateAsStaff(orderId, body),
                ApiTrace.resolve(requestContext)
        );
    }
}
