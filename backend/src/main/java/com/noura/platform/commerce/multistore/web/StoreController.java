package com.noura.platform.commerce.multistore.web;

import com.noura.platform.commerce.multistore.application.StoreService;
import com.noura.platform.commerce.multistore.domain.Store;
import com.noura.platform.commerce.multistore.domain.StoreInventory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for multi-store management.
 */
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    // === Store CRUD ===

    @GetMapping
    public List<Store> listStores() {
        return storeService.findAllStores();
    }

    @GetMapping("/active")
    public List<Store> listActiveStores() {
        return storeService.findActiveStores();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Store> getStore(@PathVariable Long id) {
        return storeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Store createStore(@RequestBody Store store) {
        return storeService.createStore(store);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Store> updateStore(@PathVariable Long id, @RequestBody Store store) {
        store.setId(id);
        return ResponseEntity.ok(storeService.updateStore(store));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateStore(@PathVariable Long id) {
        storeService.deactivateStore(id);
        return ResponseEntity.noContent().build();
    }

    // === Store Inventory ===

    @GetMapping("/{storeId}/inventory")
    public List<StoreInventory> getStoreInventory(@PathVariable Long storeId) {
        return storeService.getStoreInventory(storeId);
    }

    @GetMapping("/{storeId}/inventory/{productId}")
    public ResponseEntity<StoreInventory> getProductInventory(
            @PathVariable Long storeId,
            @PathVariable Long productId,
            @RequestParam(required = false) Long variantId) {
        return storeService.getInventory(storeId, productId, variantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{storeId}/inventory/adjust")
    public StoreInventory adjustInventory(
            @PathVariable Long storeId,
            @RequestBody InventoryAdjustRequest request) {
        return storeService.adjustInventory(
                storeId,
                request.productId(),
                request.variantId(),
                request.quantityChange(),
                request.reason()
        );
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferInventory(@RequestBody TransferRequest request) {
        storeService.transferInventory(
                request.fromStoreId(),
                request.toStoreId(),
                request.productId(),
                request.variantId(),
                request.quantity()
        );
        return ResponseEntity.ok().build();
    }

    // === DTOs ===

    public record InventoryAdjustRequest(
            Long productId,
            Long variantId,
            int quantityChange,
            String reason
    ) {}

    public record TransferRequest(
            Long fromStoreId,
            Long toStoreId,
            Long productId,
            Long variantId,
            int quantity
    ) {}
}
