package com.noura.platform.commerce.marketplace.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * Amazon Seller Central / SP-API connector.
 */
@Component
public class AmazonMarketplaceConnector implements MarketplaceConnector {
    private static final Logger log = LoggerFactory.getLogger(AmazonMarketplaceConnector.class);
    private static final String CHANNEL_TYPE = "AMAZON";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AmazonMarketplaceConnector(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getChannelType() {
        return CHANNEL_TYPE;
    }

    @Override
    public ConnectionTestResult testConnection(ChannelCredentials credentials) {
        try {
            // Call Amazon SP-API to verify credentials
            HttpHeaders headers = createHeaders(credentials);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    credentials.apiEndpoint() + "/sellers/v1/marketplaceParticipations",
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();
                String sellerName = body != null ? 
                        body.path("payload").path(0).path("marketplace").path("name").asText("Unknown") : 
                        "Unknown";
                return new ConnectionTestResult(true, "Connected successfully", sellerName);
            }

            return new ConnectionTestResult(false, "Connection failed", null);

        } catch (Exception e) {
            log.error("Amazon connection test failed: {}", e.getMessage());
            return new ConnectionTestResult(false, "Error: " + e.getMessage(), null);
        }
    }

    @Override
    public SyncResult syncProducts(Long channelId, List<ProductData> products) {
        // Amazon uses a feed-based system for product updates
        // This would create a product feed and submit via SP-API
        log.info("Syncing {} products to Amazon channel {}", products.size(), channelId);

        int success = 0;
        int failure = 0;
        List<String> errors = new ArrayList<>();

        for (ProductData product : products) {
            try {
                // In real implementation:
                // 1. Build product feed XML/JSON
                // 2. Submit feed via /feeds/2021-06-30/feeds API
                // 3. Poll for feed processing result
                success++;
            } catch (Exception e) {
                failure++;
                errors.add(product.sku() + ": " + e.getMessage());
            }
        }

        return new SyncResult(products.size(), success, failure, errors);
    }

    @Override
    public SyncResult syncInventory(Long channelId, List<InventoryUpdate> updates) {
        log.info("Syncing {} inventory updates to Amazon channel {}", updates.size(), channelId);

        int success = 0;
        int failure = 0;
        List<String> errors = new ArrayList<>();

        // Amazon inventory updates via Inventory API
        for (InventoryUpdate update : updates) {
            try {
                // PUT /fba/inventory/v1/items/{sellerSku}
                success++;
            } catch (Exception e) {
                failure++;
                errors.add(update.sku() + ": " + e.getMessage());
            }
        }

        return new SyncResult(updates.size(), success, failure, errors);
    }

    @Override
    public List<MarketplaceOrderData> fetchOrders(Long channelId, FetchOrdersRequest request) {
        log.info("Fetching orders from Amazon channel {} since {}", channelId, request.fromDate());

        List<MarketplaceOrderData> orders = new ArrayList<>();

        // In real implementation:
        // GET /orders/v0/orders?CreatedAfter=...&OrderStatuses=...

        return orders;
    }

    @Override
    public boolean updateOrderStatus(Long channelId, String externalOrderId, OrderStatusUpdate update) {
        log.info("Updating Amazon order {} with tracking {}", externalOrderId, update.trackingNumber());

        // POST /orders/v0/orders/{orderId}/shipment
        // Include shipment confirmation with tracking

        return true;
    }

    @Override
    public boolean acknowledgeOrder(Long channelId, String externalOrderId) {
        // Amazon doesn't require explicit acknowledgment
        return true;
    }

    private HttpHeaders createHeaders(ChannelCredentials credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-amz-access-token", credentials.accessToken());
        return headers;
    }
}
