package com.noura.platform.inventory.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.inventory.config.InventorySecurityConfig;
import com.noura.platform.inventory.dto.warehouse.WarehouseRequest;
import com.noura.platform.inventory.dto.warehouse.WarehouseResponse;
import com.noura.platform.inventory.service.WarehouseBinService;
import com.noura.platform.inventory.service.WarehouseService;
import com.noura.platform.testsupport.inventory.api.InventoryWebMvcSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarehouseController.class)
@Import({WarehouseController.class, InventorySecurityConfig.class, InventoryExceptionHandler.class, InventoryWebMvcSecurityTestConfig.class})
@ActiveProfiles("inventory-webmvc-test")
@TestPropertySource(properties = {
        "inventory.api.base-path=/api/inventory/v1",
        "inventory.security.dev-header-auth-enabled=false",
        "inventory.security.jwt.secret=12345678901234567890123456789012"
})
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WarehouseService warehouseService;

    @MockBean
    private WarehouseBinService warehouseBinService;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void createWarehouseAllowedForWarehouseManager() throws Exception {
        WarehouseRequest request = new WarehouseRequest(
                "WH-PNH-01",
                "Phnom Penh Main",
                "FULFILLMENT",
                "Street 1",
                null,
                "Phnom Penh",
                null,
                "12000",
                "KH",
                true
        );
        WarehouseResponse response = new WarehouseResponse(
                "wh-1",
                "WH-PNH-01",
                "Phnom Penh Main",
                "FULFILLMENT",
                "Street 1",
                null,
                "Phnom Penh",
                null,
                "12000",
                "KH",
                true,
                Instant.parse("2026-03-08T00:00:00Z"),
                Instant.parse("2026-03-08T00:00:00Z")
        );
        when(warehouseService.createWarehouse(any(WarehouseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/inventory/v1/warehouses")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.warehouseCode").value("WH-PNH-01"));
    }
}
