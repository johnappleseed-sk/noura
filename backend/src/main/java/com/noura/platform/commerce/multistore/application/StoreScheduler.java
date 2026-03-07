package com.noura.platform.commerce.multistore.application;

import com.noura.platform.commerce.multistore.domain.StoreInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled jobs for multi-store operations.
 */
@Component
@ConditionalOnProperty(name = "app.multistore.enabled", havingValue = "true")
public class StoreScheduler {
    private static final Logger log = LoggerFactory.getLogger(StoreScheduler.class);

    private final StoreService storeService;

    public StoreScheduler(StoreService storeService) {
        this.storeService = storeService;
    }

    /**
     * Check for low stock across all stores.
     * Default: every 4 hours.
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void checkLowStock() {
        log.info("Checking low stock across stores...");

        var stores = storeService.findAllActive();

        for (var store : stores) {
            try {
                List<StoreInventory> lowStock = storeService.getLowStock(store.getId());

                if (!lowStock.isEmpty()) {
                    log.warn("Store {}: {} products at low stock", store.getCode(), lowStock.size());
                    // TODO: Send alert to store manager or procurement
                }
            } catch (Exception e) {
                log.error("Failed to check low stock for store {}: {}", store.getCode(), e.getMessage());
            }
        }

        log.info("Low stock check complete.");
    }

    /**
     * Reconcile inventory counts across stores.
     * Default: Sunday at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void reconcileInventory() {
        log.info("Starting weekly inventory reconciliation...");

        // TODO: Implement reconciliation logic
        // 1. Compare physical counts vs system counts
        // 2. Flag discrepancies
        // 3. Generate reconciliation report

        log.info("Inventory reconciliation complete.");
    }
}
