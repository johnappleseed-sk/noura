package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.domain.entity.Inventory;
import com.noura.platform.domain.entity.InventoryReservation;
import com.noura.platform.domain.entity.InventoryTransaction;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductVariant;
import com.noura.platform.domain.entity.Warehouse;
import com.noura.platform.domain.enums.InventoryReservationStatus;
import com.noura.platform.domain.enums.InventoryTransactionType;
import com.noura.platform.dto.inventory.InventoryCheckItemRequest;
import com.noura.platform.dto.inventory.InventoryCheckRequest;
import com.noura.platform.dto.inventory.InventoryCheckResultDto;
import com.noura.platform.dto.inventory.InventoryReserveRequest;
import com.noura.platform.repository.InventoryRepository;
import com.noura.platform.repository.InventoryReservationRepository;
import com.noura.platform.repository.InventoryTransactionRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryReservationRepository inventoryReservationRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private WarehouseRepository warehouseRepository;

    @Test
    void reserve_shouldCreateBackorderWhenInsufficientAndAllowed() {
        InventoryServiceImpl service = service();
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        ProductVariant variant = variant(variantId, true);
        Warehouse warehouse = warehouse(warehouseId, true);
        Inventory inventory = inventory(variant, warehouse, 2, 0);

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByVariantIdAndWarehouseId(variantId, warehouseId)).thenReturn(Optional.of(inventory));
        when(inventoryReservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> {
            InventoryReservation saved = invocation.getArgument(0);
            saved.setId(reservationId);
            return saved;
        });
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reserve(new InventoryReserveRequest(variantId, warehouseId, orderId, 5, "flash sale"));

        assertEquals(InventoryReservationStatus.BACKORDERED, result.status());
        verify(inventoryRepository, never()).save(any(Inventory.class));
        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(inventoryTransactionRepository).save(txCaptor.capture());
        assertEquals(InventoryTransactionType.BACKORDER, txCaptor.getValue().getType());
        assertEquals(0, txCaptor.getValue().getChangeQuantity());
    }

    @Test
    void check_shouldReturnBackorderAvailableWhenStockMissingButAllowed() {
        InventoryServiceImpl service = service();
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        ProductVariant variant = variant(variantId, true);

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findByVariantIdAndWarehouseId(variantId, warehouseId)).thenReturn(Optional.empty());

        InventoryCheckResultDto result = service.check(
                new InventoryCheckRequest(List.of(new InventoryCheckItemRequest(variantId, warehouseId, 3)))
        );

        assertEquals(1, result.items().size());
        assertTrue(result.items().getFirst().available());
        assertTrue(result.items().getFirst().backorder());
        assertEquals(0, result.items().getFirst().availableQuantity());
    }

    @Test
    void reserve_shouldRejectWhenInsufficientAndBackorderDisabled() {
        InventoryServiceImpl service = service();
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        ProductVariant variant = variant(variantId, false);
        Warehouse warehouse = warehouse(warehouseId, true);
        Inventory inventory = inventory(variant, warehouse, 1, 0);

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByVariantIdAndWarehouseId(variantId, warehouseId)).thenReturn(Optional.of(inventory));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.reserve(new InventoryReserveRequest(variantId, warehouseId, UUID.randomUUID(), 2, null))
        );
        assertEquals("INVENTORY_INSUFFICIENT", exception.getCode());
        assertFalse(variant.getProduct().isAllowBackorder());
    }

    private InventoryServiceImpl service() {
        return new InventoryServiceImpl(
                inventoryRepository,
                inventoryReservationRepository,
                inventoryTransactionRepository,
                productVariantRepository,
                warehouseRepository
        );
    }

    private ProductVariant variant(UUID id, boolean allowBackorder) {
        Product product = new Product();
        product.setAllowBackorder(allowBackorder);
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProduct(product);
        return variant;
    }

    private Warehouse warehouse(UUID id, boolean active) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setActive(active);
        return warehouse;
    }

    private Inventory inventory(ProductVariant variant, Warehouse warehouse, int quantity, int reserved) {
        Inventory inventory = new Inventory();
        inventory.setVariant(variant);
        inventory.setWarehouse(warehouse);
        inventory.setQuantity(quantity);
        inventory.setReservedQuantity(reserved);
        return inventory;
    }
}
