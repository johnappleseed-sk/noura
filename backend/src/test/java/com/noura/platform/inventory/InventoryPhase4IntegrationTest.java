package com.noura.platform.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.EnterpriseCommerceApiApplication;
import com.noura.platform.inventory.domain.Product;
import com.noura.platform.inventory.domain.Warehouse;
import com.noura.platform.inventory.domain.WarehouseBin;
import com.noura.platform.inventory.dto.stock.InboundMovementRequest;
import com.noura.platform.inventory.dto.stock.StockMovementLineRequest;
import com.noura.platform.inventory.repository.InventoryProductRepository;
import com.noura.platform.inventory.repository.WarehouseBinRepository;
import com.noura.platform.inventory.repository.InventoryWarehouseRepository;
import com.noura.platform.inventory.service.StockMovementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EnterpriseCommerceApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local-mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InventoryPhase4IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryProductRepository productRepository;

    @Autowired
    private InventoryWarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseBinRepository warehouseBinRepository;

    @Autowired
    private StockMovementService stockMovementService;

    @Test
    void seededAdminCanLoginAndAccessCurrentUser() throws Exception {
        String token = loginAsSeededAdmin();

        mockMvc.perform(get("/api/inventory/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("inventory.admin"))
                .andExpect(jsonPath("$.data.roles[0]").exists());
    }

    @Test
    void batchSerialReportingBarcodeAuditAndWebhookEndpointsWorkWithJwt() throws Exception {
        String token = loginAsSeededAdmin();
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Product product = new Product();
        product.setSku("PH4-" + suffix);
        product.setName("Phase 4 Product " + suffix);
        product.setDescription("Phase 4 inventory test product");
        product.setStatus("ACTIVE");
        product.setBasePrice(new BigDecimal("25.00"));
        product.setCurrencyCode("USD");
        product.setBatchTracked(true);
        product.setSerialTracked(true);
        product.setActive(true);
        product = productRepository.save(product);

        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode("PH4-WH-" + suffix);
        warehouse.setName("Phase 4 Warehouse " + suffix);
        warehouse.setWarehouseType("FULFILLMENT");
        warehouse.setCountryCode("KH");
        warehouse.setActive(true);
        warehouse = warehouseRepository.save(warehouse);

        WarehouseBin bin = new WarehouseBin();
        bin.setWarehouse(warehouse);
        bin.setBinCode("PH4-BIN-" + suffix);
        bin.setBinType("STANDARD");
        bin.setPickSequence(1);
        bin.setActive(true);
        bin = warehouseBinRepository.save(bin);

        mockMvc.perform(post("/api/inventory/v1/webhooks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventCode": "stock.changed",
                                  "endpointUrl": "http://127.0.0.1:65535/inventory-hook",
                                  "active": true,
                                  "timeoutMs": 1000,
                                  "retryCount": 0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.eventCode").value("stock.changed"));

        stockMovementService.receiveInbound(new InboundMovementRequest(
                warehouse.getId(),
                bin.getId(),
                "PO",
                "PH4-PO-" + suffix,
                null,
                "Phase 4 inbound",
                List.of(new StockMovementLineRequest(
                        product.getId(),
                        new BigDecimal("2"),
                        null,
                        null,
                        null,
                        "LOT-" + suffix,
                        LocalDate.now().plusDays(30),
                        null,
                        null,
                        new BigDecimal("25.00"),
                        null,
                        List.of("SN-" + suffix + "-1", "SN-" + suffix + "-2")
                ))
        ));

        mockMvc.perform(get("/api/inventory/v1/batches")
                        .header("Authorization", "Bearer " + token)
                        .param("productId", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].lotNumber").value("LOT-" + suffix));

        mockMvc.perform(get("/api/inventory/v1/serials")
                        .header("Authorization", "Bearer " + token)
                        .param("productId", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].serialStatus").value("IN_STOCK"));

        mockMvc.perform(get("/api/inventory/v1/reports/stock-valuation")
                        .header("Authorization", "Bearer " + token)
                        .param("warehouseId", warehouse.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStockValue").value(50.0000))
                .andExpect(jsonPath("$.data.items[0].productSku").value("PH4-" + suffix));

        mockMvc.perform(get("/api/inventory/v1/reports/export")
                        .header("Authorization", "Bearer " + token)
                        .param("reportType", "stock-valuation")
                        .param("warehouseId", warehouse.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("productSku")));

        byte[] barcode = mockMvc.perform(get("/api/inventory/v1/barcodes/products/{productId}", product.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(barcode).isNotEmpty();

        mockMvc.perform(get("/api/inventory/v1/audit-logs")
                        .header("Authorization", "Bearer " + token)
                        .param("entityType", "Product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    private String loginAsSeededAdmin() throws Exception {
        String response = mockMvc.perform(post("/api/inventory/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "inventory.admin",
                                  "password": "Admin123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.path("data").path("accessToken").asText();
    }
}
