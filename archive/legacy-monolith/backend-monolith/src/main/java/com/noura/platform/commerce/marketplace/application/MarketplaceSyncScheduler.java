package com.noura.platform.commerce.marketplace.application;

import com.noura.platform.commerce.marketplace.domain.MarketplaceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduled jobs for marketplace synchronization.
 */
@Component
@ConditionalOnProperty(name = "app.marketplace.enabled", havingValue = "true")
public class MarketplaceSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(MarketplaceSyncScheduler.class);

    private final MarketplaceService marketplaceService;

    public MarketplaceSyncScheduler(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    /**
     * Fetch orders from all active marketplace channels.
     * Default: every 15 minutes.
     */
    @Scheduled(cron = "${app.scheduler.marketplace-order-sync-cron:0 */15 * * * *}")
    @ConditionalOnProperty(name = "app.marketplace.order-sync-enabled", havingValue = "true", matchIfMissing = true)
    public void syncOrders() {
        log.info("Starting marketplace order sync...");

        List<MarketplaceChannel> channels = marketplaceService.findActiveChannels();
        String fromDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE);
        String toDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        for (MarketplaceChannel channel : channels) {
            try {
                log.debug("Syncing orders from channel: {}", channel.getCode());
                var orders = marketplaceService.fetchAndImportOrders(channel.getId(), fromDate, toDate);
                log.info("Channel {}: imported {} orders", channel.getCode(), orders.size());
            } catch (Exception e) {
                log.error("Failed to sync orders from channel {}: {}", channel.getCode(), e.getMessage());
            }
        }

        log.info("Marketplace order sync complete.");
    }

    /**
     * Sync inventory levels to all active marketplace channels.
     * Default: every 2 hours.
     */
    @Scheduled(cron = "${app.scheduler.marketplace-inventory-sync-cron:0 0 */2 * * *}")
    @ConditionalOnProperty(name = "app.marketplace.inventory-sync-enabled", havingValue = "true", matchIfMissing = true)
    public void syncInventory() {
        log.info("Starting marketplace inventory sync...");

        List<MarketplaceChannel> channels = marketplaceService.findActiveChannels();

        for (MarketplaceChannel channel : channels) {
            try {
                log.debug("Syncing inventory to channel: {}", channel.getCode());
                // Get all product mappings for this channel and push inventory
                var mappings = marketplaceService.findMappingsByChannel(channel.getId());

                // TODO: Build inventory updates from current stock levels
                // This would look up actual inventory from StoreInventory or Product entities
                // For now, just log the attempt

                log.info("Channel {}: {} products to sync", channel.getCode(), mappings.size());
            } catch (Exception e) {
                log.error("Failed to sync inventory to channel {}: {}", channel.getCode(), e.getMessage());
            }
        }

        log.info("Marketplace inventory sync complete.");
    }
}
