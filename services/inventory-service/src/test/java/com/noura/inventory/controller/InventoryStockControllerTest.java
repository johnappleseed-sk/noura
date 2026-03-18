package com.noura.inventory.controller;

import com.noura.inventory.domain.enums.StockStatus;
import com.noura.inventory.dto.stock.StockLevelResponse;
import com.noura.inventory.exception.ApiExceptionHandler;
import com.noura.inventory.service.InventoryStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for {@link InventoryStockController}.
 */
@WebMvcTest(controllers = InventoryStockController.class)
@Import(ApiExceptionHandler.class)
class InventoryStockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryStockService inventoryStockService;

    /**
     * Verifies list endpoint returns wrapped paged response in API envelope format.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void listStockLevelsReturnsApiEnvelope() throws Exception {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID warehouseId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        StockLevelResponse response = new StockLevelResponse(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                productId,
                "SKU-001",
                "Inventory Product",
                warehouseId,
                "WH-01",
                "Main Warehouse",
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(16),
                BigDecimal.ZERO,
                Instant.parse("2026-03-15T10:15:30Z"),
                false,
                BigDecimal.valueOf(5),
                StockStatus.IN_STOCK,
                Instant.parse("2026-03-15T10:15:30Z")
        );
        Page<StockLevelResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        when(inventoryStockService.listStockLevels(
                eq(productId),
                eq(warehouseId),
                eq(null),
                eq(null),
                eq(null),
                eq(PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedAt")))
        )).thenReturn(page);

        mockMvc.perform(get("/api/inventory/v1/stock-levels")
                        .param("productId", productId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Stock levels"))
                .andExpect(jsonPath("$.data.content[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.content[0].warehouseId").value(warehouseId.toString()))
                .andExpect(jsonPath("$.data.content[0].quantityAvailable").value(16));
    }

    /**
     * Verifies bean validation failures are returned in standard error envelope.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void reserveStockReturnsValidationErrorForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/inventory/v1/stock-levels/reservations")
                        .contentType("application/json")
                        .content("""
                                {
                                  "productId": "11111111-1111-1111-1111-111111111111",
                                  "locationId": "22222222-2222-2222-2222-222222222222"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    /**
     * Verifies low-stock lookup returns the standard paginated envelope used by admin operations.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void lowStockReturnsPagedEnvelope() throws Exception {
        UUID productId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID warehouseId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        StockLevelResponse response = new StockLevelResponse(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                productId,
                "SKU-LOW-1",
                "Low Stock Product",
                warehouseId,
                "WH-02",
                "Overflow Warehouse",
                null,
                null,
                null,
                null,
                new BigDecimal("3.0000"),
                new BigDecimal("1.0000"),
                new BigDecimal("2.0000"),
                BigDecimal.ZERO.setScale(4),
                Instant.parse("2026-03-18T08:00:00Z"),
                true,
                new BigDecimal("5.0000"),
                StockStatus.LOW_STOCK,
                Instant.parse("2026-03-18T08:00:00Z")
        );
        Page<StockLevelResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        when(inventoryStockService.getLowStock(
                eq(PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "quantityAvailable")))
        )).thenReturn(page);

        mockMvc.perform(get("/api/inventory/v1/stock-levels/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Low stock levels"))
                .andExpect(jsonPath("$.data.content[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.content[0].stockStatus").value("LOW_STOCK"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
