package com.noura.platform.commerce.reports.web;

import com.noura.platform.commerce.b2b.application.B2BService;
import com.noura.platform.commerce.b2b.domain.POStatus;
import com.noura.platform.commerce.b2b.domain.PurchaseOrder;
import com.noura.platform.commerce.multistore.application.StoreService;
import com.noura.platform.commerce.multistore.domain.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for enterprise reports.
 */
@RestController
@RequestMapping("/api/reports")
public class EnterpriseReportsController {

    private final StoreService storeService;
    private final B2BService b2bService;

    public EnterpriseReportsController(StoreService storeService, B2BService b2bService) {
        this.storeService = storeService;
        this.b2bService = b2bService;
    }

    /**
     * Multi-store inventory summary.
     */
    @GetMapping("/stores/inventory-summary")
    public ResponseEntity<List<StoreInventorySummary>> getStoreInventorySummary() {
        List<Store> stores = storeService.findActiveStores();

        List<StoreInventorySummary> summaries = stores.stream()
                .map(store -> {
                    var inventory = storeService.getStoreInventory(store.getId());
                    int totalItems = inventory.size();
                    BigDecimal totalValue = inventory.stream()
                            .map(inv -> inv.getQuantityOnHand().multiply(BigDecimal.valueOf(10))) // Placeholder cost
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int lowStockCount = (int) inventory.stream()
                            .filter(inv -> inv.getReorderPoint() != null &&
                                    inv.getQuantityOnHand().compareTo(inv.getReorderPoint()) <= 0)
                            .count();

                    return new StoreInventorySummary(
                            store.getId(),
                            store.getCode(),
                            store.getName(),
                            totalItems,
                            totalValue,
                            lowStockCount
                    );
                })
                .toList();

        return ResponseEntity.ok(summaries);
    }

    /**
     * B2B accounts receivable summary.
     */
    @GetMapping("/b2b/accounts-receivable")
    public ResponseEntity<B2BARSummary> getAccountsReceivable() {
        List<PurchaseOrder> overdueOrders = b2bService.findOverdueOrders();

        BigDecimal totalOverdue = overdueOrders.stream()
                .map(PurchaseOrder::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get pending orders
        Page<PurchaseOrder> pendingPage = b2bService.findPurchaseOrders(
                null, POStatus.PENDING_APPROVAL, PageRequest.of(0, 100));

        BigDecimal totalPending = pendingPage.getContent().stream()
                .map(PurchaseOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get approved but unpaid
        Page<PurchaseOrder> approvedPage = b2bService.findPurchaseOrders(
                null, POStatus.APPROVED, PageRequest.of(0, 100));

        BigDecimal totalApproved = approvedPage.getContent().stream()
                .map(PurchaseOrder::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(new B2BARSummary(
                overdueOrders.size(),
                totalOverdue,
                (int) pendingPage.getTotalElements(),
                totalPending,
                (int) approvedPage.getTotalElements(),
                totalApproved
        ));
    }

    /**
     * B2B sales by company.
     */
    @GetMapping("/b2b/sales-by-company")
    public ResponseEntity<Map<String, Object>> getSalesByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PurchaseOrder> orders = b2bService.findPurchaseOrders(
                null, POStatus.CLOSED, PageRequest.of(page, size));

        // Group by company
        Map<Long, CompanySales> salesByCompany = new HashMap<>();

        for (PurchaseOrder order : orders) {
            Long companyId = order.getCompany().getId();
            salesByCompany.computeIfAbsent(companyId, k ->
                    new CompanySales(companyId, order.getCompany().getName(), 0, BigDecimal.ZERO)
            );

            CompanySales current = salesByCompany.get(companyId);
            salesByCompany.put(companyId, new CompanySales(
                    companyId,
                    current.companyName(),
                    current.orderCount() + 1,
                    current.totalSales().add(order.getTotalAmount())
            ));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", salesByCompany.values());
        result.put("totalElements", orders.getTotalElements());
        result.put("totalPages", orders.getTotalPages());

        return ResponseEntity.ok(result);
    }

    // === DTOs ===

    public record StoreInventorySummary(
            Long storeId,
            String storeCode,
            String storeName,
            int totalItems,
            BigDecimal totalValue,
            int lowStockCount
    ) {}

    public record B2BARSummary(
            int overdueCount,
            BigDecimal overdueAmount,
            int pendingCount,
            BigDecimal pendingAmount,
            int approvedCount,
            BigDecimal approvedOutstanding
    ) {}

    public record CompanySales(
            Long companyId,
            String companyName,
            int orderCount,
            BigDecimal totalSales
    ) {}
}
