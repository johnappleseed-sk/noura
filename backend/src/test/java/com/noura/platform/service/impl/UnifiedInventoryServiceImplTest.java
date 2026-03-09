package com.noura.platform.service.impl;

import com.noura.platform.commerce.api.v1.dto.inventory.StockMovementDto;
import com.noura.platform.commerce.api.v1.service.ApiInventoryService;
import com.noura.platform.domain.enums.StockMovementType;
import com.noura.platform.dto.inventory.WarehouseDto;
import com.noura.platform.dto.inventory.WarehouseRequest;
import com.noura.platform.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedInventoryServiceImplTest {

    @Mock
    private InventoryService platformInventoryService;

    @Mock
    private ApiInventoryService commerceInventoryService;

    private UnifiedInventoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UnifiedInventoryServiceImpl(platformInventoryService, provider(null), variantInventoryProvider(null));
    }

    @Test
    void createWarehouseDelegatesToPlatformInventoryService() {
        WarehouseRequest request = new WarehouseRequest("Main Warehouse", "Phnom Penh", true);
        WarehouseDto expected = new WarehouseDto(UUID.randomUUID(), "Main Warehouse", "Phnom Penh", true);
        when(platformInventoryService.createWarehouse(request)).thenReturn(expected);

        WarehouseDto actual = service.createWarehouse(request);

        assertThat(actual).isSameAs(expected);
        verify(platformInventoryService).createWarehouse(request);
    }

    @Test
    void listCommerceMovementsDelegatesWhenLegacyServiceIsAvailable() {
        var pageable = PageRequest.of(0, 20);
        Page<StockMovementDto> expected = new PageImpl<>(List.of(new StockMovementDto(
                1L,
                42L,
                "Jasmine Rice",
                "RIC-001",
                5,
                BigDecimal.ONE,
                "USD",
                "RECEIVE",
                "PURCHASE",
                "PO-1",
                LocalDateTime.now(),
                7L,
                "T-01",
                "received"
        )));
        when(commerceInventoryService.listMovements(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 7),
                42L,
                StockMovementType.RECEIVE,
                pageable
        )).thenReturn(expected);
        service = new UnifiedInventoryServiceImpl(platformInventoryService, provider(commerceInventoryService), variantInventoryProvider(null));

        Page<StockMovementDto> actual = service.listCommerceMovements(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 7),
                42L,
                StockMovementType.RECEIVE,
                pageable
        );

        assertThat(actual).isSameAs(expected);
        verify(commerceInventoryService).listMovements(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 7),
                42L,
                StockMovementType.RECEIVE,
                pageable
        );
    }

    @Test
    void listCommerceMovementsFailsFastWhenLegacyServiceIsInactive() {
        assertThatThrownBy(() -> service.listCommerceMovements(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 7),
                42L,
                StockMovementType.SALE,
                PageRequest.of(0, 20)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Legacy commerce inventory service is not active");
    }

    private ObjectProvider<ApiInventoryService> provider(ApiInventoryService inventoryService) {
        return new ObjectProvider<>() {
            @Override
            public ApiInventoryService getObject(Object... args) {
                return getIfAvailable();
            }

            @Override
            public ApiInventoryService getIfAvailable() {
                return inventoryService;
            }

            @Override
            public ApiInventoryService getIfUnique() {
                return inventoryService;
            }

            @Override
            public ApiInventoryService getObject() {
                if (inventoryService == null) {
                    throw new IllegalStateException("Legacy commerce inventory service is not active in the current runtime profile.");
                }
                return inventoryService;
            }

            @Override
            public Iterator<ApiInventoryService> iterator() {
                return inventoryService == null ? List.<ApiInventoryService>of().iterator() : List.of(inventoryService).iterator();
            }

            @Override
            public Stream<ApiInventoryService> stream() {
                return inventoryService == null ? Stream.empty() : Stream.of(inventoryService);
            }
        };
    }

    private ObjectProvider<com.noura.platform.commerce.service.InventoryService> variantInventoryProvider(
            com.noura.platform.commerce.service.InventoryService svc) {
        return new ObjectProvider<>() {
            @Override
            public com.noura.platform.commerce.service.InventoryService getObject(Object... args) { return getIfAvailable(); }
            @Override
            public com.noura.platform.commerce.service.InventoryService getIfAvailable() { return svc; }
            @Override
            public com.noura.platform.commerce.service.InventoryService getIfUnique() { return svc; }
            @Override
            public com.noura.platform.commerce.service.InventoryService getObject() {
                if (svc == null) throw new IllegalStateException("Commerce variant inventory service is not active in the current runtime profile.");
                return svc;
            }
            @Override
            public Iterator<com.noura.platform.commerce.service.InventoryService> iterator() {
                return svc == null ? List.<com.noura.platform.commerce.service.InventoryService>of().iterator() : List.of(svc).iterator();
            }
            @Override
            public Stream<com.noura.platform.commerce.service.InventoryService> stream() {
                return svc == null ? Stream.empty() : Stream.of(svc);
            }
        };
    }
}
