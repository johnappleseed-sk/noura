package com.noura.platform.commerce.multistore.application;

import com.noura.platform.commerce.multistore.domain.Store;
import com.noura.platform.commerce.multistore.domain.StoreInventory;
import com.noura.platform.commerce.multistore.infrastructure.StoreInventoryRepo;
import com.noura.platform.commerce.multistore.infrastructure.StoreRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing stores and store-specific inventory.
 */
@Service
@Transactional
public class StoreService {
    private final StoreRepo storeRepo;
    private final StoreInventoryRepo inventoryRepo;

    public StoreService(StoreRepo storeRepo, StoreInventoryRepo inventoryRepo) {
        this.storeRepo = storeRepo;
        this.inventoryRepo = inventoryRepo;
    }

    // === Store Management ===

    public Store createStore(Store store) {
        if (storeRepo.existsByCode(store.getCode())) {
            throw new IllegalArgumentException("Store code already exists: " + store.getCode());
        }
        return storeRepo.save(store);
    }

    public Store updateStore(Long id, Store updates) {
        Store store = storeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + id));

        if (updates.getName() != null) store.setName(updates.getName());
        if (updates.getDescription() != null) store.setDescription(updates.getDescription());
        if (updates.getEmail() != null) store.setEmail(updates.getEmail());
        if (updates.getPhone() != null) store.setPhone(updates.getPhone());
        if (updates.getAddressLine1() != null) store.setAddressLine1(updates.getAddressLine1());
        if (updates.getCity() != null) store.setCity(updates.getCity());
        if (updates.getState() != null) store.setState(updates.getState());
        if (updates.getPostalCode() != null) store.setPostalCode(updates.getPostalCode());
        if (updates.getTimezone() != null) store.setTimezone(updates.getTimezone());

        store.setUpdatedAt(LocalDateTime.now());
        return storeRepo.save(store);
    }

    @Transactional(readOnly = true)
    public Optional<Store> findById(Long id) {
        return storeRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Store> findByCode(String code) {
        return storeRepo.findByCode(code);
    }

    @Transactional(readOnly = true)
    public List<Store> findAllActive() {
        return storeRepo.findByActiveTrueOrderByName();
    }

    public void deactivateStore(Long id) {
        Store store = storeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + id));
        store.setActive(false);
        store.setUpdatedAt(LocalDateTime.now());
        storeRepo.save(store);
    }

    // === Store Inventory ===

    public StoreInventory adjustInventory(Long storeId, Long productId, BigDecimal adjustment) {
        StoreInventory inv = inventoryRepo.findByStoreIdAndProductId(storeId, productId)
                .orElseGet(() -> {
                    StoreInventory newInv = new StoreInventory();
                    newInv.setStore(storeRepo.getReferenceById(storeId));
                    newInv.setProductId(productId);
                    return newInv;
                });

        inv.setQuantityOnHand(inv.getQuantityOnHand().add(adjustment));
        inv.setUpdatedAt(LocalDateTime.now());
        return inventoryRepo.save(inv);
    }

    public void reserveInventory(Long storeId, Long productId, BigDecimal quantity) {
        StoreInventory inv = inventoryRepo.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new IllegalArgumentException("No inventory record"));

        if (inv.getQuantityAvailable().compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient inventory");
        }

        inv.setQuantityReserved(inv.getQuantityReserved().add(quantity));
        inv.setUpdatedAt(LocalDateTime.now());
        inventoryRepo.save(inv);
    }

    public void releaseReservation(Long storeId, Long productId, BigDecimal quantity) {
        StoreInventory inv = inventoryRepo.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new IllegalArgumentException("No inventory record"));

        inv.setQuantityReserved(inv.getQuantityReserved().subtract(quantity));
        inv.setUpdatedAt(LocalDateTime.now());
        inventoryRepo.save(inv);
    }

    @Transactional(readOnly = true)
    public BigDecimal getQuantityAcrossStores(Long productId) {
        BigDecimal total = inventoryRepo.sumQuantityByProductId(productId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<StoreInventory> getLowStock(Long storeId) {
        return inventoryRepo.findLowStockByStoreId(storeId);
    }

    /**
     * Transfer inventory between stores.
     */
    public void transferInventory(Long fromStoreId, Long toStoreId, Long productId, BigDecimal quantity) {
        adjustInventory(fromStoreId, productId, quantity.negate());
        adjustInventory(toStoreId, productId, quantity);
    }

    /**
     * Transfer inventory between stores (int overload for controller).
     */
    public void transferInventory(Long fromStoreId, Long toStoreId, Long productId, Long variantId, int quantity) {
        transferInventory(fromStoreId, toStoreId, productId, BigDecimal.valueOf(quantity));
    }

    /**
     * Adjust inventory with reason tracking.
     */
    public StoreInventory adjustInventory(Long storeId, Long productId, Long variantId, int quantityChange, String reason) {
        // For now, variantId is ignored - extend as needed
        return adjustInventory(storeId, productId, BigDecimal.valueOf(quantityChange));
    }

    /**
     * Get inventory for a specific product at a store.
     */
    @Transactional(readOnly = true)
    public Optional<StoreInventory> getInventory(Long storeId, Long productId, Long variantId) {
        return inventoryRepo.findByStoreIdAndProductId(storeId, productId);
    }

    /**
     * Get all inventory for a store.
     */
    @Transactional(readOnly = true)
    public List<StoreInventory> getStoreInventory(Long storeId) {
        return inventoryRepo.findByStoreId(storeId);
    }

    /**
     * Update store (using entity).
     */
    public Store updateStore(Store store) {
        return updateStore(store.getId(), store);
    }

    /**
     * Find all stores.
     */
    @Transactional(readOnly = true)
    public List<Store> findAllStores() {
        return storeRepo.findAll();
    }

    /**
     * Find all active stores.
     */
    @Transactional(readOnly = true)
    public List<Store> findActiveStores() {
        return findAllActive();
    }
}
