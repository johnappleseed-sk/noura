package com.noura.platform.commerce.marketplace.application;

import com.noura.platform.commerce.marketplace.domain.*;
import com.noura.platform.commerce.marketplace.infrastructure.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing marketplace channels and synchronization.
 */
@Service
@Transactional
public class MarketplaceService {
    private static final Logger log = LoggerFactory.getLogger(MarketplaceService.class);

    private final MarketplaceChannelRepo channelRepo;
    private final MarketplaceProductMappingRepo productMappingRepo;
    private final MarketplaceOrderRepo orderRepo;
    private final Map<String, MarketplaceConnector> connectors;

    public MarketplaceService(
            MarketplaceChannelRepo channelRepo,
            MarketplaceProductMappingRepo productMappingRepo,
            MarketplaceOrderRepo orderRepo,
            List<MarketplaceConnector> connectorList) {
        this.channelRepo = channelRepo;
        this.productMappingRepo = productMappingRepo;
        this.orderRepo = orderRepo;

        // Build connector map by channel type
        this.connectors = new HashMap<>();
        for (MarketplaceConnector connector : connectorList) {
            connectors.put(connector.getChannelType(), connector);
        }
    }

    // === Channel Management ===

    public MarketplaceChannel createChannel(MarketplaceChannel channel) {
        if (channelRepo.existsByCode(channel.getCode())) {
            throw new IllegalArgumentException("Channel code already exists: " + channel.getCode());
        }
        return channelRepo.save(channel);
    }

    public MarketplaceConnector.ConnectionTestResult testChannel(Long channelId) {
        MarketplaceChannel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        MarketplaceConnector connector = getConnector(channel.getType());

        MarketplaceConnector.ChannelCredentials credentials = new MarketplaceConnector.ChannelCredentials(
                channel.getApiKey(),
                channel.getApiSecret(),
                channel.getAccessToken(),
                channel.getRefreshToken(),
                channel.getMerchantId(),
                channel.getApiEndpoint()
        );

        return connector.testConnection(credentials);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceChannel> findActiveChannels() {
        return channelRepo.findByActiveTrue();
    }

    // === Product Sync ===

    public MarketplaceConnector.SyncResult syncProducts(Long channelId, List<MarketplaceConnector.ProductData> products) {
        MarketplaceChannel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        MarketplaceConnector connector = getConnector(channel.getType());
        MarketplaceConnector.SyncResult result = connector.syncProducts(channelId, products);

        channel.setLastProductSync(LocalDateTime.now());
        channel.setUpdatedAt(LocalDateTime.now());
        channelRepo.save(channel);

        return result;
    }

    public MarketplaceConnector.SyncResult syncInventory(Long channelId, List<MarketplaceConnector.InventoryUpdate> updates) {
        MarketplaceChannel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        MarketplaceConnector connector = getConnector(channel.getType());
        MarketplaceConnector.SyncResult result = connector.syncInventory(channelId, updates);

        channel.setLastInventorySync(LocalDateTime.now());
        channel.setUpdatedAt(LocalDateTime.now());
        channelRepo.save(channel);

        return result;
    }

    // === Order Import ===

    public List<MarketplaceOrder> fetchAndImportOrders(Long channelId, String fromDate, String toDate) {
        MarketplaceChannel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        MarketplaceConnector connector = getConnector(channel.getType());

        MarketplaceConnector.FetchOrdersRequest request = new MarketplaceConnector.FetchOrdersRequest(
                fromDate, toDate, null, 100
        );

        List<MarketplaceConnector.MarketplaceOrderData> externalOrders = connector.fetchOrders(channelId, request);

        List<MarketplaceOrder> importedOrders = new ArrayList<>();

        for (MarketplaceConnector.MarketplaceOrderData orderData : externalOrders) {
            // Check if already imported
            if (orderRepo.existsByChannelIdAndExternalOrderId(channelId, orderData.externalOrderId())) {
                log.debug("Order {} already exists, skipping", orderData.externalOrderId());
                continue;
            }

            MarketplaceOrder order = new MarketplaceOrder();
            order.setChannel(channel);
            order.setExternalOrderId(orderData.externalOrderId());
            order.setExternalStatus(orderData.status());
            order.setBuyerName(orderData.buyerName());
            order.setBuyerEmail(orderData.buyerEmail());

            if (orderData.shippingAddress() != null) {
                order.setShipName(orderData.shippingAddress().name());
                order.setShipAddressLine1(orderData.shippingAddress().line1());
                order.setShipCity(orderData.shippingAddress().city());
                order.setShipState(orderData.shippingAddress().state());
                order.setShipPostalCode(orderData.shippingAddress().postalCode());
                order.setShipCountryCode(orderData.shippingAddress().countryCode());
            }

            order.setSubtotal(orderData.subtotal());
            order.setShippingAmount(orderData.shippingAmount());
            order.setTaxAmount(orderData.taxAmount());
            order.setTotalAmount(orderData.totalAmount());
            order.setCurrencyCode(orderData.currencyCode());
            order.setShippingService(orderData.shippingService());
            order.setRawOrderData(orderData.rawJson());
            order.setImportStatus(ImportStatus.PENDING);

            importedOrders.add(orderRepo.save(order));
        }

        channel.setLastOrderSync(LocalDateTime.now());
        channel.setUpdatedAt(LocalDateTime.now());
        channelRepo.save(channel);

        return importedOrders;
    }

    // === Order Status Updates ===

    public boolean updateExternalOrderStatus(Long marketplaceOrderId, String trackingNumber, String carrier) {
        MarketplaceOrder order = orderRepo.findById(marketplaceOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        MarketplaceChannel channel = order.getChannel();
        MarketplaceConnector connector = getConnector(channel.getType());

        MarketplaceConnector.OrderStatusUpdate update = new MarketplaceConnector.OrderStatusUpdate(
                "SHIPPED",
                trackingNumber,
                carrier,
                LocalDateTime.now().toString()
        );

        return connector.updateOrderStatus(channel.getId(), order.getExternalOrderId(), update);
    }

    // === Product Mapping ===

    public MarketplaceProductMapping createMapping(Long channelId, Long productId, Long variantId, 
                                                    String externalProductId, String externalSku) {
        MarketplaceChannel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        MarketplaceProductMapping mapping = new MarketplaceProductMapping();
        mapping.setChannel(channel);
        mapping.setProductId(productId);
        mapping.setVariantId(variantId);
        mapping.setExternalProductId(externalProductId);
        mapping.setExternalSku(externalSku);
        mapping.setStatus(ListingStatus.PENDING);

        return productMappingRepo.save(mapping);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceProductMapping> findMappingsByChannel(Long channelId) {
        return productMappingRepo.findByChannelId(channelId);
    }

    private MarketplaceConnector getConnector(ChannelType type) {
        MarketplaceConnector connector = connectors.get(type.name());
        if (connector == null) {
            throw new IllegalStateException("No connector available for channel type: " + type);
        }
        return connector;
    }
}
