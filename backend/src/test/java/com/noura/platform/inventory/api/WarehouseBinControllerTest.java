package com.noura.platform.inventory.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.inventory.config.InventorySecurityConfig;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinRequest;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinResponse;
import com.noura.platform.inventory.dto.warehouse.WarehouseSummaryResponse;
import com.noura.platform.inventory.service.WarehouseBinService;
import com.noura.platform.testsupport.inventory.api.InventoryWebMvcSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({WarehouseController.class, WarehouseBinController.class})
@Import({WarehouseController.class, WarehouseBinController.class, InventorySecurityConfig.class, InventoryExceptionHandler.class, InventoryWebMvcSecurityTestConfig.class})
@TestPropertySource(properties = {
        "inventory.api.base-path=/api/inventory/v1",
        "inventory.security.dev-header-auth-enabled=false",
        "inventory.security.jwt.secret=12345678901234567890123456789012"
})
class WarehouseBinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.noura.platform.inventory.service.WarehouseService warehouseService;

    @MockBean
    private WarehouseBinService warehouseBinService;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void createWarehouseBinReturnsCreated() throws Exception {
        WarehouseBinRequest request = new WarehouseBinRequest("A-01-01", "PICK", "A", "01", "STANDARD", null, null, 1, true);
        WarehouseBinResponse response = new WarehouseBinResponse(
                "bin-1",
                new WarehouseSummaryResponse("wh-1", "WH-PNH-01", "Phnom Penh Main"),
                "A-01-01",
                "PICK",
                "A",
                "01",
                "STANDARD",
                null,
                null,
                1,
                true,
                Instant.parse("2026-03-08T00:00:00Z"),
                Instant.parse("2026-03-08T00:00:00Z")
        );
        when(warehouseBinService.createBin(eq("wh-1"), any(WarehouseBinRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/inventory/v1/warehouses/wh-1/bins")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.binCode").value("A-01-01"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void updateWarehouseBinForbiddenForViewer() throws Exception {
        WarehouseBinRequest request = new WarehouseBinRequest("A-01-01", "PICK", "A", "01", "STANDARD", null, null, 1, true);

        mockMvc.perform(put("/api/inventory/v1/bins/bin-1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }
}
