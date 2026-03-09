package com.noura.platform.inventory;

import com.noura.platform.EnterpriseCommerceApiApplication;
import com.noura.platform.inventory.domain.Product;
import com.noura.platform.inventory.domain.ReorderAlert;
import com.noura.platform.inventory.domain.StockPolicy;
import com.noura.platform.inventory.domain.Warehouse;
import com.noura.platform.inventory.domain.WarehouseBin;
import com.noura.platform.inventory.dto.stock.AdjustmentMovementLineRequest;
import com.noura.platform.inventory.dto.stock.AdjustmentMovementRequest;
import com.noura.platform.inventory.dto.stock.InboundMovementRequest;
import com.noura.platform.inventory.dto.stock.OutboundMovementRequest;
import com.noura.platform.inventory.dto.stock.StockLevelFilter;
import com.noura.platform.inventory.dto.stock.StockMovementLineRequest;
import com.noura.platform.inventory.dto.stock.StockMovementResponse;
import com.noura.platform.inventory.dto.stock.TransferMovementRequest;
import com.noura.platform.inventory.repository.ReorderAlertRepository;
import com.noura.platform.inventory.repository.SerialNumberRepository;
import com.noura.platform.inventory.repository.StockLevelRepository;
import com.noura.platform.inventory.repository.StockPolicyRepository;
import com.noura.platform.inventory.repository.WarehouseBinRepository;
import com.noura.platform.inventory.repository.InventoryWarehouseRepository;
import com.noura.platform.inventory.repository.InventoryProductRepository;
import com.noura.platform.inventory.service.StockLevelService;
import com.noura.platform.inventory.service.StockMovementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EnterpriseCommerceApiApplication.class)
@ActiveProfiles("local-mysql")
@Transactional(transactionManager = "inventoryTransactionManager")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StockMovementServiceIntegrationTest {

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private StockLevelService stockLevelService;

    @Autowired
    private InventoryProductRepository productRepository;

    @Autowired
    private InventoryWarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseBinRepository warehouseBinRepository;

    @Autowired
    private StockPolicyRepository stockPolicyRepository;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private ReorderAlertRepository reorderAlertRepository;

    @Autowired
    private SerialNumberRepository serialNumberRepository;

    @Test
    void outboundAllocatesBatchTrackedStockUsingFefoAndOpensLowStockAlert() {
        String suffix = uniqueSuffix();
        Product product = createProduct("BATCH-" + suffix, true, false);
        Warehouse warehouse = createWarehouse("WH-" + suffix);
        WarehouseBin bin = createBin(warehouse, "BIN-" + suffix);
        createPolicy(product, warehouse, new BigDecimal("4"));

        stockMovementService.receiveInbound(new InboundMovementRequest(
                warehouse.getId(),
                bin.getId(),
                "PO",
                "PO-" + suffix,
                null,
                "Initial receipt",
                List.of(
                        new StockMovementLineRequest(
                                product.getId(),
                                new BigDecimal("5"),
                                null,
                                null,
                                null,
                                "LOT-" + suffix + "-A",
                                LocalDate.now().plusDays(5),
                                null,
                                null,
                                new BigDecimal("10.00"),
                                null,
                                null
                        ),
                        new StockMovementLineRequest(
                                product.getId(),
                                new BigDecimal("5"),
                                null,
                                null,
                                null,
                                "LOT-" + suffix + "-B",
                                LocalDate.now().plusDays(20),
                                null,
                                null,
                                new BigDecimal("10.00"),
                                null,
                                null
                        )
                )
        ));

        StockMovementResponse outbound = stockMovementService.shipOutbound(new OutboundMovementRequest(
                warehouse.getId(),
                bin.getId(),
                "SO",
                "SO-" + suffix,
                null,
                "Customer shipment",
                List.of(new StockMovementLineRequest(
                        product.getId(),
                        new BigDecimal("6"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        ));

        assertThat(outbound.movementType()).isEqualTo("OUTBOUND");
        assertThat(outbound.lines()).hasSize(2);
        assertThat(outbound.lines().get(0).lotNumber()).isEqualTo("LOT-" + suffix + "-A");
        assertThat(outbound.lines().get(0).quantity()).isEqualByComparingTo("5");
        assertThat(outbound.lines().get(1).lotNumber()).isEqualTo("LOT-" + suffix + "-B");
        assertThat(outbound.lines().get(1).quantity()).isEqualByComparingTo("1");
        assertThat(stockLevelRepository.sumAvailableByProductAndWarehouse(product.getId(), warehouse.getId()))
                .isEqualByComparingTo("4");

        ReorderAlert alert = reorderAlertRepository
                .findFirstByProduct_IdAndWarehouse_IdAndResolvedAtIsNullOrderByCreatedAtDesc(product.getId(), warehouse.getId())
                .orElseThrow();
        assertThat(alert.getAlertStatus()).isEqualTo("OPEN");
        assertThat(alert.getCurrentQuantity()).isEqualByComparingTo("4");

        var lowStockPage = stockLevelService.listStockLevels(
                new StockLevelFilter(product.getId(), warehouse.getId(), null, null, true),
                org.springframework.data.domain.PageRequest.of(0, 10)
        );
        assertThat(lowStockPage.getContent()).hasSize(2);
        assertThat(lowStockPage.getContent()).allMatch(level -> level.lowStock());
    }

    @Test
    void transferMovesSerialTrackedUnitsBetweenWarehouses() {
        String suffix = uniqueSuffix();
        Product product = createProduct("SERIAL-" + suffix, false, true);
        Warehouse sourceWarehouse = createWarehouse("SRC-" + suffix);
        WarehouseBin sourceBin = createBin(sourceWarehouse, "SRC-BIN-" + suffix);
        Warehouse destinationWarehouse = createWarehouse("DST-" + suffix);
        WarehouseBin destinationBin = createBin(destinationWarehouse, "DST-BIN-" + suffix);

        stockMovementService.receiveInbound(new InboundMovementRequest(
                sourceWarehouse.getId(),
                sourceBin.getId(),
                "PO",
                "PO-" + suffix,
                null,
                "Serialized receipt",
                List.of(new StockMovementLineRequest(
                        product.getId(),
                        new BigDecimal("2"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of("SN-" + suffix + "-1", "SN-" + suffix + "-2")
                ))
        ));

        StockMovementResponse transfer = stockMovementService.transferStock(new TransferMovementRequest(
                sourceWarehouse.getId(),
                sourceBin.getId(),
                destinationWarehouse.getId(),
                destinationBin.getId(),
                "TRANSFER",
                "TR-" + suffix,
                null,
                "Rebalancing stock",
                List.of(new StockMovementLineRequest(
                        product.getId(),
                        new BigDecimal("2"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of("SN-" + suffix + "-1", "SN-" + suffix + "-2")
                ))
        ));

        assertThat(transfer.movementType()).isEqualTo("TRANSFER");
        assertThat(transfer.lines()).hasSize(2);
        assertThat(stockLevelRepository.sumAvailableByProductAndWarehouse(product.getId(), sourceWarehouse.getId()))
                .isEqualByComparingTo("0");
        assertThat(stockLevelRepository.sumAvailableByProductAndWarehouse(product.getId(), destinationWarehouse.getId()))
                .isEqualByComparingTo("2");

        var serialOne = serialNumberRepository.findBySerialNumberAndDeletedAtIsNull("SN-" + suffix + "-1").orElseThrow();
        var serialTwo = serialNumberRepository.findBySerialNumberAndDeletedAtIsNull("SN-" + suffix + "-2").orElseThrow();
        assertThat(serialOne.getWarehouse().getId()).isEqualTo(destinationWarehouse.getId());
        assertThat(serialOne.getBin().getId()).isEqualTo(destinationBin.getId());
        assertThat(serialTwo.getWarehouse().getId()).isEqualTo(destinationWarehouse.getId());
        assertThat(serialTwo.getBin().getId()).isEqualTo(destinationBin.getId());
    }

    @Test
    void adjustmentsChangeStockAndResolveLowStockAlerts() {
        String suffix = uniqueSuffix();
        Product product = createProduct("ADJ-" + suffix, false, false);
        Warehouse warehouse = createWarehouse("ADJ-WH-" + suffix);
        WarehouseBin bin = createBin(warehouse, "ADJ-BIN-" + suffix);
        createPolicy(product, warehouse, new BigDecimal("3"));

        stockMovementService.receiveInbound(new InboundMovementRequest(
                warehouse.getId(),
                bin.getId(),
                "PO",
                "PO-" + suffix,
                null,
                null,
                List.of(new StockMovementLineRequest(
                        product.getId(),
                        new BigDecimal("5"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        ));

        stockMovementService.adjustStock(new AdjustmentMovementRequest(
                warehouse.getId(),
                bin.getId(),
                "COUNT",
                "ADJ-" + suffix + "-OUT",
                null,
                "Cycle count decrease",
                List.of(new AdjustmentMovementLineRequest(
                        product.getId(),
                        new BigDecimal("-4"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        ));

        ReorderAlert openAlert = reorderAlertRepository
                .findFirstByProduct_IdAndWarehouse_IdAndResolvedAtIsNullOrderByCreatedAtDesc(product.getId(), warehouse.getId())
                .orElseThrow();
        assertThat(openAlert.getCurrentQuantity()).isEqualByComparingTo("1");
        assertThat(openAlert.getAlertStatus()).isEqualTo("OPEN");

        stockMovementService.adjustStock(new AdjustmentMovementRequest(
                warehouse.getId(),
                bin.getId(),
                "COUNT",
                "ADJ-" + suffix + "-IN",
                null,
                "Cycle count increase",
                List.of(new AdjustmentMovementLineRequest(
                        product.getId(),
                        new BigDecimal("5"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        ));

        assertThat(stockLevelRepository.sumAvailableByProductAndWarehouse(product.getId(), warehouse.getId()))
                .isEqualByComparingTo("6");
        assertThat(reorderAlertRepository
                .findFirstByProduct_IdAndWarehouse_IdAndResolvedAtIsNullOrderByCreatedAtDesc(product.getId(), warehouse.getId()))
                .isEmpty();
    }

    private Product createProduct(String sku, boolean batchTracked, boolean serialTracked) {
        Product product = new Product();
        product.setSku(sku);
        product.setName("Product " + sku);
        product.setDescription("Integration test product");
        product.setStatus("ACTIVE");
        product.setBasePrice(new BigDecimal("12.50"));
        product.setCurrencyCode("USD");
        product.setBatchTracked(batchTracked);
        product.setSerialTracked(serialTracked);
        product.setActive(true);
        return productRepository.save(product);
    }

    private Warehouse createWarehouse(String code) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode(code);
        warehouse.setName("Warehouse " + code);
        warehouse.setWarehouseType("FULFILLMENT");
        warehouse.setCountryCode("KH");
        warehouse.setActive(true);
        return warehouseRepository.save(warehouse);
    }

    private WarehouseBin createBin(Warehouse warehouse, String code) {
        WarehouseBin bin = new WarehouseBin();
        bin.setWarehouse(warehouse);
        bin.setBinCode(code);
        bin.setBinType("STANDARD");
        bin.setPickSequence(1);
        bin.setActive(true);
        return warehouseBinRepository.save(bin);
    }

    private StockPolicy createPolicy(Product product, Warehouse warehouse, BigDecimal threshold) {
        StockPolicy policy = new StockPolicy();
        policy.setProduct(product);
        policy.setWarehouse(warehouse);
        policy.setLowStockThreshold(threshold);
        policy.setReorderPoint(threshold);
        policy.setReorderQuantity(new BigDecimal("10"));
        policy.setAllowBackorder(false);
        return stockPolicyRepository.save(policy);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
