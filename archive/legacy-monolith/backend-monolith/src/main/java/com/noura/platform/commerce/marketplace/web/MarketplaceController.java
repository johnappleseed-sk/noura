package com.noura.platform.commerce.marketplace.web;

import com.noura.platform.commerce.marketplace.application.MarketplaceConnector;
import com.noura.platform.commerce.marketplace.application.MarketplaceService;
import com.noura.platform.commerce.marketplace.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for marketplace integration management.
 */
@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    // === Channel Management ===

    @GetMapping("/channels")
    public List<MarketplaceChannel> listChannels() {
        return marketplaceService.findActiveChannels();
    }

    @PostMapping("/channels")
    public MarketplaceChannel createChannel(@RequestBody MarketplaceChannel channel) {
        return marketplaceService.createChannel(channel);
    }

    @PostMapping("/channels/{id}/test")
    public ResponseEntity<MarketplaceConnector.ConnectionTestResult> testChannel(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.testChannel(id));
    }

    // === Product Sync ===

    @PostMapping("/channels/{channelId}/sync/products")
    public ResponseEntity<MarketplaceConnector.SyncResult> syncProducts(
            @PathVariable Long channelId,
            @RequestBody List<MarketplaceConnector.ProductData> products) {
        return ResponseEntity.ok(marketplaceService.syncProducts(channelId, products));
    }

    @PostMapping("/channels/{channelId}/sync/inventory")
    public ResponseEntity<MarketplaceConnector.SyncResult> syncInventory(
            @PathVariable Long channelId,
            @RequestBody List<MarketplaceConnector.InventoryUpdate> updates) {
        return ResponseEntity.ok(marketplaceService.syncInventory(channelId, updates));
    }

    // === Product Mappings ===

    @GetMapping("/channels/{channelId}/mappings")
    public List<MarketplaceProductMapping> getChannelMappings(@PathVariable Long channelId) {
        return marketplaceService.findMappingsByChannel(channelId);
    }

    @PostMapping("/channels/{channelId}/mappings")
    public MarketplaceProductMapping createMapping(
            @PathVariable Long channelId,
            @RequestBody CreateMappingRequest request) {
        return marketplaceService.createMapping(
                channelId,
                request.productId(),
                request.variantId(),
                request.externalProductId(),
                request.externalSku()
        );
    }

    // === Order Import ===

    @PostMapping("/channels/{channelId}/orders/fetch")
    public List<MarketplaceOrder> fetchOrders(
            @PathVariable Long channelId,
            @RequestBody FetchOrdersRequest request) {
        return marketplaceService.fetchAndImportOrders(channelId, request.fromDate(), request.toDate());
    }

    @PostMapping("/orders/{orderId}/ship")
    public ResponseEntity<Boolean> updateOrderShipment(
            @PathVariable Long orderId,
            @RequestBody ShipmentUpdateRequest request) {
        boolean success = marketplaceService.updateExternalOrderStatus(
                orderId, request.trackingNumber(), request.carrier());
        return ResponseEntity.ok(success);
    }

    // === DTOs ===

    public record CreateMappingRequest(
            Long productId,
            Long variantId,
            String externalProductId,
            String externalSku
    ) {}

    public record FetchOrdersRequest(String fromDate, String toDate) {}

    public record ShipmentUpdateRequest(String trackingNumber, String carrier) {}
}
