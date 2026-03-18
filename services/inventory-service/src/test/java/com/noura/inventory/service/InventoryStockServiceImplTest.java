package com.noura.inventory.service;

import com.noura.inventory.domain.entity.InventoryStockLevel;
import com.noura.inventory.domain.entity.InventoryStockMovement;
import com.noura.inventory.domain.enums.StockMovementType;
import com.noura.inventory.dto.stock.StockAdjustmentRequest;
import com.noura.inventory.dto.stock.StockDeductionRequest;
import com.noura.inventory.dto.stock.StockOperationResponse;
import com.noura.inventory.dto.stock.StockReleaseReservationRequest;
import com.noura.inventory.dto.stock.StockReservationRequest;
import com.noura.inventory.exception.InventoryOperationException;
import com.noura.inventory.repository.InventoryStockLevelRepository;
import com.noura.inventory.repository.InventoryStockMovementRepository;
import com.noura.inventory.service.impl.InventoryStockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InventoryStockServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class InventoryStockServiceImplTest {

    @Mock
    private InventoryStockLevelRepository stockLevelRepository;

    @Mock
    private InventoryStockMovementRepository movementRepository;

    private InventoryStockServiceImpl service;

    /**
     * Initializes service under test with mocked dependencies.
     */
    @BeforeEach
    void setUp() {
        service = new InventoryStockServiceImpl(stockLevelRepository, movementRepository);
    }

    /**
     * Verifies reserve operation updates reserved and available quantities and writes movement.
     */
    @Test
    void reserveIncreasesReservedAndCreatesMovement() {
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        InventoryStockLevel stockLevel = stockLevel(productId, warehouseId, "10.0000", "2.0000", "5.0000");

        when(stockLevelRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId))
                .thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(any(InventoryStockLevel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(InventoryStockMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockOperationResponse response = service.reserve(
                new StockReservationRequest(productId, warehouseId, new BigDecimal("3"), "CHECKOUT", "ORDER", "ORD-001", null),
                "user-1"
        );

        assertThat(response.movementType()).isEqualTo(StockMovementType.RESERVE);
        assertThat(response.stockLevel().quantityReserved()).isEqualByComparingTo("5.0000");
        assertThat(response.stockLevel().quantityAvailable()).isEqualByComparingTo("5.0000");
        verify(stockLevelRepository).save(stockLevel);
        verify(movementRepository).save(any(InventoryStockMovement.class));
    }

    /**
     * Verifies reserved-consumption deduction fails when reserved quantity is insufficient.
     */
    @Test
    void deductWithReservedConsumptionFailsWhenReservedIsInsufficient() {
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        InventoryStockLevel stockLevel = stockLevel(productId, warehouseId, "8.0000", "1.0000", "2.0000");

        when(stockLevelRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId))
                .thenReturn(Optional.of(stockLevel));

        assertThatThrownBy(() -> service.deduct(
                new StockDeductionRequest(productId, warehouseId, new BigDecimal("2"), true, "ORDER", "ORDER", "ORD-002", null),
                "user-2"
        ))
                .isInstanceOf(InventoryOperationException.class)
                .hasMessageContaining("Reserved quantity is lower");

        verify(movementRepository, never()).save(any(InventoryStockMovement.class));
    }

    /**
     * Verifies adjustment creates baseline stock level when product/location row is missing.
     */
    @Test
    void adjustCreatesStockLevelWhenMissing() {
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        when(stockLevelRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId))
                .thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(InventoryStockLevel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(InventoryStockMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockOperationResponse response = service.adjust(
                new StockAdjustmentRequest(
                        productId,
                        warehouseId,
                        new BigDecimal("7.5"),
                        new BigDecimal("4"),
                        "CYCLE_COUNT",
                        "MANUAL",
                        "REF-1",
                        "Initial load"
                ),
                "user-3"
        );

        assertThat(response.movementType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(response.stockLevel().quantityOnHand()).isEqualByComparingTo("7.5000");
        assertThat(response.stockLevel().quantityAvailable()).isEqualByComparingTo("7.5000");
        assertThat(response.stockLevel().lowStockThreshold()).isEqualByComparingTo("4.0000");
        verify(stockLevelRepository, times(2)).save(any(InventoryStockLevel.class));
        verify(movementRepository).save(any(InventoryStockMovement.class));
    }

    /**
     * Verifies release operation returns quantity to availability and writes a negative-delta movement.
     */
    @Test
    void releaseReservationDecreasesReservedAndCreatesReleaseMovement() {
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        InventoryStockLevel stockLevel = stockLevel(productId, warehouseId, "10.0000", "5.0000", "3.0000");

        when(stockLevelRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId))
                .thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(any(InventoryStockLevel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(InventoryStockMovement.class)))
                .thenAnswer(invocation -> {
                    InventoryStockMovement movement = invocation.getArgument(0, InventoryStockMovement.class);
                    movement.setId(UUID.randomUUID());
                    return movement;
                });

        StockOperationResponse response = service.releaseReservation(
                new StockReleaseReservationRequest(
                        productId,
                        warehouseId,
                        new BigDecimal("2"),
                        "CHECKOUT_CANCELLED",
                        "ORDER",
                        "ORD-003",
                        "Customer cancelled checkout"
                ),
                "user-4"
        );

        ArgumentCaptor<InventoryStockMovement> movementCaptor = ArgumentCaptor.forClass(InventoryStockMovement.class);

        assertThat(response.movementType()).isEqualTo(StockMovementType.RELEASE);
        assertThat(response.stockLevel().quantityReserved()).isEqualByComparingTo("3.0000");
        assertThat(response.stockLevel().quantityAvailable()).isEqualByComparingTo("7.0000");
        verify(movementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getQuantityDelta()).isEqualByComparingTo("-2.0000");
    }

    /**
     * Creates in-memory stock-level fixture used by tests.
     *
     * @param productId product identifier
     * @param warehouseId warehouse/location identifier
     * @param onHand on-hand quantity
     * @param reserved reserved quantity
     * @param threshold low-stock threshold
     * @return prepared stock-level fixture
     */
    private InventoryStockLevel stockLevel(UUID productId, UUID warehouseId, String onHand, String reserved, String threshold) {
        InventoryStockLevel stockLevel = new InventoryStockLevel();
        stockLevel.setProductId(productId);
        stockLevel.setWarehouseId(warehouseId);
        stockLevel.setProductSku(productId.toString());
        stockLevel.setProductName("Product");
        stockLevel.setWarehouseCode(warehouseId.toString());
        stockLevel.setWarehouseName("Warehouse");
        stockLevel.setQuantityOnHand(new BigDecimal(onHand));
        stockLevel.setQuantityReserved(new BigDecimal(reserved));
        stockLevel.setLowStockThreshold(new BigDecimal(threshold));
        stockLevel.recalculateAvailability();
        return stockLevel;
    }
}
