package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Inventory;
import com.noura.platform.domain.entity.InventoryTransaction;
import com.noura.platform.domain.entity.ProductVariant;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.StoreTenant;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.entity.UserStoreAssignment;
import com.noura.platform.domain.entity.Warehouse;
import com.noura.platform.domain.enums.InventoryTransactionType;
import com.noura.platform.domain.enums.MerchantContractStatus;
import com.noura.platform.domain.enums.StoreTenantStatus;
import com.noura.platform.dto.inventory.InventoryAdjustRequest;
import com.noura.platform.dto.inventory.InventoryLevelDto;
import com.noura.platform.dto.inventory.InventorySummaryDto;
import com.noura.platform.repository.InventoryRepository;
import com.noura.platform.repository.InventoryTransactionRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.UserStoreAssignmentRepository;
import com.noura.platform.repository.WarehouseRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.StoreInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreInventoryServiceImpl implements StoreInventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WarehouseRepository warehouseRepository;
    private final StoreRepository storeRepository;
    private final StoreTenantRepository storeTenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserStoreAssignmentRepository userStoreAssignmentRepository;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER','MANAGER','INVENTORY_MANAGER','WAREHOUSE_MANAGER')")
    public InventorySummaryDto stock(UUID storeId, UUID variantId) {
        requireStoreAccess(storeId);
        requireStoreContractValid(storeId);
        List<InventoryLevelDto> levels = inventoryRepository.findByVariantIdAndWarehouseStoreId(variantId, storeId)
                .stream()
                .map(this::toLevelDto)
                .toList();
        if (levels.isEmpty()) {
            ensureVariantExists(variantId);
        }
        return new InventorySummaryDto(variantId, levels);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER','MANAGER','INVENTORY_MANAGER','WAREHOUSE_MANAGER')")
    public InventoryLevelDto adjust(UUID storeId, InventoryAdjustRequest request) {
        requireStoreAccess(storeId);
        requireStoreContractValid(storeId);
        ProductVariant variant = productVariantRepository.findById(request.variantId())
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Variant not found"));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new NotFoundException("WAREHOUSE_NOT_FOUND", "Warehouse not found"));
        if (warehouse.getStore() == null || !storeId.equals(warehouse.getStore().getId())) {
            throw new ForbiddenException("WAREHOUSE_SCOPE_INVALID", "Warehouse does not belong to this store");
        }

        Inventory inventory = inventoryRepository.findByVariantIdAndWarehouseId(variant.getId(), warehouse.getId())
                .orElseGet(() -> createInventory(variant, warehouse));
        int nextQuantity = inventory.getQuantity() + request.changeQuantity();
        if (nextQuantity < 0) {
            throw new BadRequestException("INVENTORY_NEGATIVE", "Adjustment would make stock negative");
        }
        if (nextQuantity < inventory.getReservedQuantity()) {
            throw new BadRequestException("INVENTORY_RESERVED_CONFLICT", "Stock cannot be lower than reserved quantity");
        }
        inventory.setQuantity(nextQuantity);
        if (request.reorderPoint() != null) {
            inventory.setReorderPoint(Math.max(0, request.reorderPoint()));
        }
        Inventory saved = inventoryRepository.save(inventory);
        recordTransaction(saved.getVariant(), saved.getWarehouse(), request.changeQuantity(), request.reason());
        return toLevelDto(saved);
    }

    private void recordTransaction(ProductVariant variant, Warehouse warehouse, int changeQuantity, String note) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setVariant(variant);
        tx.setWarehouse(warehouse);
        tx.setChangeQuantity(changeQuantity);
        tx.setType(InventoryTransactionType.ADJUSTMENT);
        tx.setNote(note == null || note.isBlank() ? null : note.trim());
        inventoryTransactionRepository.save(tx);
    }

    private Inventory createInventory(ProductVariant variant, Warehouse warehouse) {
        Inventory inventory = new Inventory();
        inventory.setVariant(variant);
        inventory.setWarehouse(warehouse);
        inventory.setQuantity(0);
        inventory.setReservedQuantity(0);
        inventory.setReorderPoint(0);
        return inventory;
    }

    private InventoryLevelDto toLevelDto(Inventory inventory) {
        int available = Math.max(0, inventory.getQuantity() - inventory.getReservedQuantity());
        return new InventoryLevelDto(
                inventory.getId(),
                inventory.getVariant().getId(),
                inventory.getWarehouse().getId(),
                inventory.getWarehouse().getName(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                available,
                inventory.getReorderPoint()
        );
    }

    private void ensureVariantExists(UUID variantId) {
        if (!productVariantRepository.existsById(variantId)) {
            throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
        }
    }

    private void requireStoreAccess(UUID storeId) {
        if (isPlatformAdmin()) {
            return;
        }
        UserAccount user = currentUser();
        UserStoreAssignment assignment = userStoreAssignmentRepository.findByUserIdAndStoreIdAndActiveTrue(user.getId(), storeId)
                .orElse(null);
        if (assignment == null) {
            throw new ForbiddenException("STORE_ACCESS_DENIED", "You are not assigned to this store");
        }
    }

    private void requireStoreContractValid(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        if (!store.isActive()) {
            throw new ForbiddenException("STORE_INACTIVE", "Store is inactive");
        }
        StoreTenant tenant = storeTenantRepository.findByStoreId(storeId).orElse(null);
        if (tenant == null) {
            return;
        }
        if (tenant.getStatus() != StoreTenantStatus.ACTIVE) {
            throw new ForbiddenException("STORE_TENANT_INACTIVE", "Store tenant is not active");
        }
        if (tenant.getContract() == null || tenant.getContract().getStatus() != MerchantContractStatus.APPROVED) {
            throw new ForbiddenException("STORE_CONTRACT_NOT_APPROVED", "Store contract is not approved");
        }
        LocalDate today = LocalDate.now();
        if (tenant.getContract().getStartDate() != null && today.isBefore(tenant.getContract().getStartDate())) {
            throw new ForbiddenException("STORE_CONTRACT_NOT_STARTED", "Store contract has not started");
        }
        if (tenant.getContract().getEndDate() != null && today.isAfter(tenant.getContract().getEndDate())) {
            throw new ForbiddenException("STORE_CONTRACT_EXPIRED", "Store contract has expired");
        }
    }

    private boolean isPlatformAdmin() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(granted -> {
            String authority = granted.getAuthority();
            return "ROLE_ADMIN".equals(authority) || "ROLE_SUPER_ADMIN".equals(authority);
        });
    }

    private UserAccount currentUser() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }
}

