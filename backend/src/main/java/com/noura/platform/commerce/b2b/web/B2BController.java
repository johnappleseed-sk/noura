package com.noura.platform.commerce.b2b.web;

import com.noura.platform.commerce.b2b.application.B2BService;
import com.noura.platform.commerce.b2b.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for B2B commerce operations.
 */
@RestController
@RequestMapping("/api/b2b")
public class B2BController {

    private final B2BService b2bService;

    public B2BController(B2BService b2bService) {
        this.b2bService = b2bService;
    }

    // === Companies ===

    @GetMapping("/companies")
    public Page<Company> listCompanies(
            @RequestParam(required = false) CompanyStatus status,
            Pageable pageable) {
        return b2bService.findCompanies(status, pageable);
    }

    @GetMapping("/companies/{id}")
    public ResponseEntity<Company> getCompany(@PathVariable Long id) {
        return b2bService.findCompanyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/companies")
    public Company createCompany(@RequestBody Company company) {
        return b2bService.createCompany(company);
    }

    @PutMapping("/companies/{id}")
    public ResponseEntity<Company> updateCompany(@PathVariable Long id, @RequestBody Company company) {
        company.setId(id);
        return ResponseEntity.ok(b2bService.updateCompany(company));
    }

    @PostMapping("/companies/{id}/approve")
    public ResponseEntity<Company> approveCompany(@PathVariable Long id) {
        return ResponseEntity.ok(b2bService.approveCompany(id));
    }

    @PostMapping("/companies/{id}/suspend")
    public ResponseEntity<Company> suspendCompany(@PathVariable Long id) {
        return ResponseEntity.ok(b2bService.suspendCompany(id));
    }

    // === Contacts ===

    @GetMapping("/companies/{companyId}/contacts")
    public List<CompanyContact> getCompanyContacts(@PathVariable Long companyId) {
        return b2bService.getCompanyContacts(companyId);
    }

    @PostMapping("/companies/{companyId}/contacts")
    public CompanyContact addContact(@PathVariable Long companyId, @RequestBody CompanyContact contact) {
        return b2bService.addContact(companyId, contact);
    }

    // === Price Lists ===

    @GetMapping("/price-lists")
    public List<PriceList> listPriceLists(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return b2bService.findPriceLists(activeOnly);
    }

    @GetMapping("/price-lists/{id}")
    public ResponseEntity<PriceList> getPriceList(@PathVariable Long id) {
        return b2bService.findPriceListById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/price-lists")
    public PriceList createPriceList(@RequestBody PriceList priceList) {
        return b2bService.createPriceList(priceList);
    }

    @PostMapping("/price-lists/{priceListId}/items")
    public PriceListItem addPriceListItem(
            @PathVariable Long priceListId,
            @RequestBody PriceListItem item) {
        return b2bService.addPriceListItem(priceListId, item);
    }

    // === Pricing ===

    @GetMapping("/pricing")
    public ResponseEntity<BigDecimal> getEffectivePrice(
            @RequestParam Long companyId,
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(defaultValue = "1") int quantity) {
        BigDecimal price = b2bService.getEffectivePrice(companyId, productId, variantId, quantity);
        return ResponseEntity.ok(price);
    }

    // === Purchase Orders ===

    @GetMapping("/purchase-orders")
    public Page<PurchaseOrder> listPurchaseOrders(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) POStatus status,
            Pageable pageable) {
        return b2bService.findPurchaseOrders(companyId, status, pageable);
    }

    @GetMapping("/purchase-orders/{id}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrder(@PathVariable Long id) {
        return b2bService.findPurchaseOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/purchase-orders")
    public PurchaseOrder createDraftOrder(@RequestBody CreatePORequest request) {
        return b2bService.createDraftOrder(request.companyId(), request.customerPoNumber());
    }

    @PostMapping("/purchase-orders/{orderId}/items")
    public PurchaseOrderItem addOrderItem(
            @PathVariable Long orderId,
            @RequestBody AddItemRequest request) {
        return b2bService.addOrderItem(
                orderId,
                request.productId(),
                request.variantId(),
                request.quantity()
        );
    }

    @PostMapping("/purchase-orders/{id}/submit")
    public ResponseEntity<PurchaseOrder> submitOrder(@PathVariable Long id) {
        return ResponseEntity.ok(b2bService.submitOrder(id));
    }

    @PostMapping("/purchase-orders/{id}/approve")
    public ResponseEntity<PurchaseOrder> approveOrder(
            @PathVariable Long id,
            @RequestParam String approvedBy) {
        return ResponseEntity.ok(b2bService.approveOrder(id, approvedBy));
    }

    @PostMapping("/purchase-orders/{id}/reject")
    public ResponseEntity<PurchaseOrder> rejectOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(b2bService.rejectOrder(id, reason));
    }

    @PostMapping("/purchase-orders/{id}/cancel")
    public ResponseEntity<PurchaseOrder> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(b2bService.cancelOrder(id));
    }

    // === DTOs ===

    public record CreatePORequest(Long companyId, String customerPoNumber) {}
    public record AddItemRequest(Long productId, Long variantId, int quantity) {}
}
