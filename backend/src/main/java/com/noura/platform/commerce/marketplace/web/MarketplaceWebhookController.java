package com.noura.platform.commerce.marketplace.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook handlers for marketplace notifications.
 */
@RestController
@RequestMapping("/api/webhooks/marketplace")
public class MarketplaceWebhookController {
    private static final Logger log = LoggerFactory.getLogger(MarketplaceWebhookController.class);

    private final ObjectMapper objectMapper;

    public MarketplaceWebhookController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Handle Shopify webhooks.
     */
    @PostMapping("/shopify")
    public ResponseEntity<String> handleShopifyWebhook(
            @RequestHeader("X-Shopify-Topic") String topic,
            @RequestHeader("X-Shopify-Shop-Domain") String shopDomain,
            @RequestHeader(value = "X-Shopify-Hmac-SHA256", required = false) String hmac,
            @RequestBody String payload) {

        log.info("Shopify webhook received: {} from {}", topic, shopDomain);

        try {
            JsonNode data = objectMapper.readTree(payload);

            switch (topic) {
                case "orders/create" -> handleShopifyOrderCreated(shopDomain, data);
                case "orders/updated" -> handleShopifyOrderUpdated(shopDomain, data);
                case "orders/cancelled" -> handleShopifyOrderCancelled(shopDomain, data);
                case "products/update" -> handleShopifyProductUpdated(shopDomain, data);
                case "inventory_levels/update" -> handleShopifyInventoryUpdate(shopDomain, data);
                default -> log.warn("Unhandled Shopify topic: {}", topic);
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Failed to process Shopify webhook: {}", e.getMessage());
            return ResponseEntity.ok("OK"); // Return 200 to prevent retries
        }
    }

    /**
     * Handle Amazon SP-API notifications (via SQS or EventBridge).
     */
    @PostMapping("/amazon")
    public ResponseEntity<String> handleAmazonWebhook(
            @RequestHeader(value = "x-amz-sns-message-type", required = false) String messageType,
            @RequestBody String payload) {

        log.info("Amazon notification received");

        try {
            JsonNode data = objectMapper.readTree(payload);

            // Handle SNS subscription confirmation
            if ("SubscriptionConfirmation".equals(messageType)) {
                String subscribeUrl = data.path("SubscribeURL").asText();
                log.info("Amazon SNS subscription confirmation needed: {}", subscribeUrl);
                // TODO: Auto-confirm subscription
                return ResponseEntity.ok("OK");
            }

            String notificationType = data.path("notificationType").asText();

            switch (notificationType) {
                case "ORDER_CHANGE" -> handleAmazonOrderChange(data);
                case "FULFILLMENT_ORDER_STATUS" -> handleAmazonFulfillmentStatus(data);
                case "REPORT_PROCESSING_FINISHED" -> handleAmazonReportReady(data);
                default -> log.warn("Unhandled Amazon notification: {}", notificationType);
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Failed to process Amazon webhook: {}", e.getMessage());
            return ResponseEntity.ok("OK");
        }
    }

    /**
     * Handle eBay notifications.
     */
    @PostMapping("/ebay")
    public ResponseEntity<String> handleEbayWebhook(
            @RequestHeader(value = "X-EBAY-SIGNATURE", required = false) String signature,
            @RequestBody String payload) {

        log.info("eBay notification received");

        try {
            JsonNode data = objectMapper.readTree(payload);
            String topic = data.path("metadata").path("topic").asText();

            switch (topic) {
                case "MARKETPLACE_ACCOUNT_DELETION" -> handleEbayAccountDeletion(data);
                default -> {
                    String eventType = data.path("notification").path("eventType").asText();
                    handleEbayEvent(eventType, data);
                }
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Failed to process eBay webhook: {}", e.getMessage());
            return ResponseEntity.ok("OK");
        }
    }

    // === Shopify Handlers ===

    private void handleShopifyOrderCreated(String shop, JsonNode order) {
        String orderId = order.path("id").asText();
        log.info("New Shopify order from {}: {}", shop, orderId);
        // TODO: Import order using MarketplaceService
    }

    private void handleShopifyOrderUpdated(String shop, JsonNode order) {
        String orderId = order.path("id").asText();
        String status = order.path("financial_status").asText();
        log.info("Shopify order updated from {}: {} -> {}", shop, orderId, status);
        // TODO: Update order status
    }

    private void handleShopifyOrderCancelled(String shop, JsonNode order) {
        String orderId = order.path("id").asText();
        log.info("Shopify order cancelled from {}: {}", shop, orderId);
        // TODO: Handle cancellation
    }

    private void handleShopifyProductUpdated(String shop, JsonNode product) {
        String productId = product.path("id").asText();
        log.info("Shopify product updated from {}: {}", shop, productId);
    }

    private void handleShopifyInventoryUpdate(String shop, JsonNode data) {
        String inventoryItemId = data.path("inventory_item_id").asText();
        int available = data.path("available").asInt();
        log.info("Shopify inventory update from {}: item {} = {}", shop, inventoryItemId, available);
    }

    // === Amazon Handlers ===

    private void handleAmazonOrderChange(JsonNode data) {
        String orderId = data.path("payload").path("AmazonOrderId").asText();
        String status = data.path("payload").path("OrderStatus").asText();
        log.info("Amazon order change: {} -> {}", orderId, status);
    }

    private void handleAmazonFulfillmentStatus(JsonNode data) {
        String orderId = data.path("payload").path("FulfillmentOrderId").asText();
        String status = data.path("payload").path("FulfillmentOrderStatus").asText();
        log.info("Amazon fulfillment status: {} -> {}", orderId, status);
    }

    private void handleAmazonReportReady(JsonNode data) {
        String reportId = data.path("payload").path("reportId").asText();
        log.info("Amazon report ready: {}", reportId);
    }

    // === eBay Handlers ===

    private void handleEbayAccountDeletion(JsonNode data) {
        String userId = data.path("notification").path("userId").asText();
        log.info("eBay account deletion request for user: {}", userId);
        // TODO: Handle GDPR deletion request
    }

    private void handleEbayEvent(String eventType, JsonNode data) {
        log.info("eBay event: {}", eventType);
    }
}
