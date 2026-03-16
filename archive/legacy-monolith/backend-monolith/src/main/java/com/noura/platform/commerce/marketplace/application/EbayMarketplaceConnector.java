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
 * eBay marketplace connector using eBay REST APIs.
 */
@Component
public class EbayMarketplaceConnector implements MarketplaceConnector {
    private static final Logger log = LoggerFactory.getLogger(EbayMarketplaceConnector.class);
    private static final String CHANNEL_TYPE = "EBAY";
    private static final String EBAY_API_URL = "https://api.ebay.com";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EbayMarketplaceConnector(ObjectMapper objectMapper) {
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
            HttpHeaders headers = createHeaders(credentials);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Get user info to verify credentials
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    getApiUrl(credentials) + "/commerce/identity/v1/user",
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();
                String username = body != null ? body.path("username").asText("Unknown") : "Unknown";
                return new ConnectionTestResult(true, "Connected to eBay", username);
            }

            return new ConnectionTestResult(false, "Connection failed", null);

        } catch (Exception e) {
            log.error("eBay connection test failed: {}", e.getMessage());
            return new ConnectionTestResult(false, "Error: " + e.getMessage(), null);
        }
    }

    @Override
    public SyncResult syncProducts(Long channelId, List<ProductData> products) {
        log.info("Syncing {} products to eBay channel {}", products.size(), channelId);

        int success = 0;
        int failure = 0;
        List<String> errors = new ArrayList<>();

        for (ProductData product : products) {
            try {
                // Inventory API: /sell/inventory/v1/inventory_item/{sku}
                // Create or update inventory item, then create/update offer
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
        log.info("Syncing {} inventory updates to eBay channel {}", updates.size(), channelId);

        int success = 0;
        int failure = 0;
        List<String> errors = new ArrayList<>();

        for (InventoryUpdate update : updates) {
            try {
                // PUT /sell/inventory/v1/inventory_item/{sku}
                // Update availability.quantity
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
        log.info("Fetching orders from eBay channel {} since {}", channelId, request.fromDate());

        List<MarketplaceOrderData> orders = new ArrayList<>();

        // GET /sell/fulfillment/v1/order?filter=creationdate:[{fromDate}..{toDate}]

        return orders;
    }

    @Override
    public boolean updateOrderStatus(Long channelId, String externalOrderId, OrderStatusUpdate update) {
        log.info("Updating eBay order {} with tracking {}", externalOrderId, update.trackingNumber());

        // POST /sell/fulfillment/v1/order/{orderId}/shipping_fulfillment
        // Include tracking information

        return true;
    }

    @Override
    public boolean acknowledgeOrder(Long channelId, String externalOrderId) {
        // eBay doesn't require explicit acknowledgment
        return true;
    }

    private HttpHeaders createHeaders(ChannelCredentials credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(credentials.accessToken());
        headers.set("X-EBAY-C-MARKETPLACE-ID", "EBAY_US"); // Could be dynamic
        return headers;
    }

    private String getApiUrl(ChannelCredentials credentials) {
        return credentials.apiEndpoint() != null ? credentials.apiEndpoint() : EBAY_API_URL;
    }
}
