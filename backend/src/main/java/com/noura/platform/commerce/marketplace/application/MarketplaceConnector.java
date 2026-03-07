package com.noura.platform.commerce.marketplace.application;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface for marketplace channel connectors.
 * Each marketplace (Amazon, eBay, etc.) implements this interface.
 */
public interface MarketplaceConnector {

    /**
     * Get the channel type this connector handles.
     */
    String getChannelType();

    /**
     * Test the connection to the marketplace.
     */
    ConnectionTestResult testConnection(ChannelCredentials credentials);

    /**
     * Sync products to the marketplace.
     */
    SyncResult syncProducts(Long channelId, List<ProductData> products);

    /**
     * Update inventory levels on the marketplace.
     */
    SyncResult syncInventory(Long channelId, List<InventoryUpdate> updates);

    /**
     * Fetch new orders from the marketplace.
     */
    List<MarketplaceOrderData> fetchOrders(Long channelId, FetchOrdersRequest request);

    /**
     * Update order status/tracking on the marketplace.
     */
    boolean updateOrderStatus(Long channelId, String externalOrderId, OrderStatusUpdate update);

    /**
     * Acknowledge/confirm order receipt.
     */
    boolean acknowledgeOrder(Long channelId, String externalOrderId);

    // === DTOs ===

    record ChannelCredentials(
            String apiKey,
            String apiSecret,
            String accessToken,
            String refreshToken,
            String merchantId,
            String apiEndpoint
    ) {}

    record ConnectionTestResult(
            boolean success,
            String message,
            String sellerName
    ) {}

    record SyncResult(
            int totalProcessed,
            int successCount,
            int failureCount,
            List<String> errors
    ) {}

    record ProductData(
            Long productId,
            String sku,
            String name,
            String description,
            BigDecimal price,
            int quantity,
            String imageUrl,
            String brand,
            String category
    ) {}

    record InventoryUpdate(
            String externalProductId,
            String sku,
            int quantity
    ) {}

    record MarketplaceOrderData(
            String externalOrderId,
            String status,
            String buyerName,
            String buyerEmail,
            ShippingAddress shippingAddress,
            List<OrderLineItem> items,
            BigDecimal subtotal,
            BigDecimal shippingAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String currencyCode,
            String shippingService,
            String orderDate,
            String rawJson
    ) {}

    record ShippingAddress(
            String name,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String countryCode,
            String phone
    ) {}

    record OrderLineItem(
            String externalProductId,
            String sku,
            String title,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    record FetchOrdersRequest(
            String fromDate,
            String toDate,
            String status,
            int limit
    ) {}

    record OrderStatusUpdate(
            String status,
            String trackingNumber,
            String carrier,
            String shipDate
    ) {}
}
