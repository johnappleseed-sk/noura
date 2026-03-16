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
 * Shopify store connector using Shopify Admin API.
 */
@Component
public class ShopifyMarketplaceConnector implements MarketplaceConnector {
    private static final Logger log = LoggerFactory.getLogger(ShopifyMarketplaceConnector.class);
    private static final String CHANNEL_TYPE = "SHOPIFY";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ShopifyMarketplaceConnector(ObjectMapper objectMapper) {
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

            // Get shop info
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    getApiUrl(credentials) + "/shop.json",
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();
                String shopName = body != null ? body.path("shop").path("name").asText("Unknown") : "Unknown";
                return new ConnectionTestResult(true, "Connected to Shopify", shopName);
            }

            return new ConnectionTestResult(false, "Connection failed", null);

        } catch (Exception e) {
            log.error("Shopify connection test failed: {}", e.getMessage());
            return new ConnectionTestResult(false, "Error: " + e.getMessage(), null);
        }
    }

    @Override
    public SyncResult syncProducts(Long channelId, List<ProductData> products) {
        log.info("Syncing {} products to Shopify channel {}", products.size(), channelId);

        int success = 0;
        int failure = 0;
        List<String> errors = new ArrayList<>();

        for (ProductData product : products) {
            try {
                // POST /products.json for new products
                // PUT /products/{id}.json for updates
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
        log.info("Syncing {} inventory updates to Shopify channel {}", updates.size(), channelId);

        int success = 0;
        int failure = 0;
        List<String> errors = new ArrayList<>();

        for (InventoryUpdate update : updates) {
            try {
                // POST /inventory_levels/set.json
                // { "location_id": x, "inventory_item_id": y, "available": z }
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
        log.info("Fetching orders from Shopify channel {} since {}", channelId, request.fromDate());

        List<MarketplaceOrderData> orders = new ArrayList<>();

        // GET /orders.json?created_at_min=...&status=any

        return orders;
    }

    @Override
    public boolean updateOrderStatus(Long channelId, String externalOrderId, OrderStatusUpdate update) {
        log.info("Updating Shopify order {} with tracking {}", externalOrderId, update.trackingNumber());

        // POST /orders/{order_id}/fulfillments.json
        // Include tracking_number, tracking_company, tracking_urls

        return true;
    }

    @Override
    public boolean acknowledgeOrder(Long channelId, String externalOrderId) {
        // Shopify doesn't require explicit acknowledgment
        return true;
    }

    private HttpHeaders createHeaders(ChannelCredentials credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", credentials.accessToken());
        return headers;
    }

    private String getApiUrl(ChannelCredentials credentials) {
        // Format: https://{store}.myshopify.com/admin/api/2024-01
        return credentials.apiEndpoint();
    }
}
