package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.ApiException;
import com.noura.platform.inventory.domain.DataExchangeJob;
import com.noura.platform.inventory.domain.IamUser;
import com.noura.platform.inventory.domain.StockLevel;
import com.noura.platform.inventory.domain.StockPolicy;
import com.noura.platform.inventory.dto.report.InventoryTurnoverItemResponse;
import com.noura.platform.inventory.dto.report.InventoryTurnoverReportResponse;
import com.noura.platform.inventory.dto.report.LowStockReportItemResponse;
import com.noura.platform.inventory.dto.report.StockValuationItemResponse;
import com.noura.platform.inventory.dto.report.StockValuationReportResponse;
import com.noura.platform.inventory.repository.DataExchangeJobRepository;
import com.noura.platform.inventory.repository.IamUserRepository;
import com.noura.platform.inventory.repository.StockLevelRepository;
import com.noura.platform.inventory.repository.StockMovementLineRepository;
import com.noura.platform.inventory.repository.StockPolicyRepository;
import com.noura.platform.inventory.security.InventoryIdentityService;
import com.noura.platform.inventory.service.InventoryReportingService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InventoryReportingServiceImpl implements InventoryReportingService {

    private final StockLevelRepository stockLevelRepository;
    private final StockPolicyRepository stockPolicyRepository;
    private final StockMovementLineRepository stockMovementLineRepository;
    private final DataExchangeJobRepository dataExchangeJobRepository;
    private final InventoryIdentityService inventoryIdentityService;
    private final IamUserRepository iamUserRepository;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public StockValuationReportResponse getStockValuationReport(String warehouseId, String productId) {
        List<StockValuationItemResponse> items = stockLevelRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(warehouseId)) {
                predicates.add(cb.equal(root.join("warehouse").get("id"), warehouseId));
            }
            if (StringUtils.hasText(productId)) {
                predicates.add(cb.equal(root.join("product").get("id"), productId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().map(this::toValuationItem).toList();
        BigDecimal total = items.stream()
                .map(StockValuationItemResponse::stockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StockValuationReportResponse(total, items);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public List<LowStockReportItemResponse> getLowStockReport(String warehouseId) {
        return stockLevelRepository.findAll((root, query, cb) -> {
            if (!StringUtils.hasText(warehouseId)) {
                return cb.conjunction();
            }
            return cb.equal(root.join("warehouse").get("id"), warehouseId);
        }).stream()
                .map(this::toLowStockItem)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public InventoryTurnoverReportResponse getTurnoverReport(Instant dateFrom, Instant dateTo) {
        Instant effectiveFrom = dateFrom != null ? dateFrom : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant effectiveTo = dateTo != null ? dateTo : Instant.now();
        List<InventoryTurnoverItemResponse> items = stockMovementLineRepository.summarizeOutboundByProduct(effectiveFrom, effectiveTo)
                .stream()
                .map(row -> {
                    String productId = (String) row[0];
                    BigDecimal outboundQuantity = (BigDecimal) row[3];
                    BigDecimal currentAvailable = stockLevelRepository.sumAvailableByProduct(productId);
                    BigDecimal turnoverRatio = currentAvailable.signum() > 0
                            ? outboundQuantity.divide(currentAvailable, 4, java.math.RoundingMode.HALF_UP)
                            : outboundQuantity;
                    return new InventoryTurnoverItemResponse(
                            productId,
                            (String) row[1],
                            (String) row[2],
                            outboundQuantity,
                            currentAvailable,
                            turnoverRatio
                    );
                })
                .toList();
        return new InventoryTurnoverReportResponse(effectiveFrom, effectiveTo, items);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public byte[] exportCsv(String reportType, String warehouseId, String productId, Instant dateFrom, Instant dateTo) {
        String normalizedType = reportType == null ? "" : reportType.trim().toLowerCase(Locale.ROOT);
        String csv;
        switch (normalizedType) {
            case "stock-valuation" -> csv = exportStockValuationCsv(getStockValuationReport(warehouseId, productId));
            case "low-stock" -> csv = exportLowStockCsv(getLowStockReport(warehouseId));
            case "turnover" -> csv = exportTurnoverCsv(getTurnoverReport(dateFrom, dateTo));
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "REPORT_TYPE_INVALID", "Unsupported reportType");
        }
        recordExport(normalizedType);
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    private void recordExport(String reportType) {
        DataExchangeJob job = new DataExchangeJob();
        job.setJobType("EXPORT");
        job.setEntityType(reportType);
        job.setFileFormat("CSV");
        job.setStoragePath("inline:response");
        InventoryIdentityService.InventoryCurrentUserSnapshot currentUser = inventoryIdentityService.getCurrentUserSnapshot();
        if (currentUser != null) {
            IamUser requestedBy = iamUserRepository.findByIdAndDeletedAtIsNull(currentUser.userId()).orElse(null);
            job.setRequestedBy(requestedBy);
        }
        job.setJobStatus("COMPLETED");
        job.setStartedAt(Instant.now());
        job.setCompletedAt(Instant.now());
        dataExchangeJobRepository.save(job);
    }

    private StockValuationItemResponse toValuationItem(StockLevel stockLevel) {
        BigDecimal unitPrice = stockLevel.getProduct().getBasePrice();
        BigDecimal stockValue = unitPrice.multiply(stockLevel.getQuantityAvailable());
        return new StockValuationItemResponse(
                stockLevel.getProduct().getId(),
                stockLevel.getProduct().getSku(),
                stockLevel.getProduct().getName(),
                stockLevel.getWarehouse().getId(),
                stockLevel.getWarehouse().getWarehouseCode(),
                stockLevel.getBin() != null ? stockLevel.getBin().getId() : null,
                stockLevel.getBin() != null ? stockLevel.getBin().getBinCode() : null,
                stockLevel.getQuantityAvailable(),
                unitPrice,
                stockValue
        );
    }

    private LowStockReportItemResponse toLowStockItem(StockLevel stockLevel) {
        StockPolicy stockPolicy = stockPolicyRepository.findByProduct_IdAndWarehouse_Id(
                        stockLevel.getProduct().getId(),
                        stockLevel.getWarehouse().getId()
                )
                .or(() -> stockPolicyRepository.findByProduct_IdAndWarehouseIsNull(stockLevel.getProduct().getId()))
                .orElse(null);
        if (stockPolicy == null || stockPolicy.getLowStockThreshold() == null || stockPolicy.getLowStockThreshold().signum() <= 0) {
            return null;
        }
        if (stockLevel.getQuantityAvailable().compareTo(stockPolicy.getLowStockThreshold()) > 0) {
            return null;
        }
        return new LowStockReportItemResponse(
                stockLevel.getProduct().getId(),
                stockLevel.getProduct().getSku(),
                stockLevel.getProduct().getName(),
                stockLevel.getWarehouse().getId(),
                stockLevel.getWarehouse().getWarehouseCode(),
                stockLevel.getQuantityAvailable(),
                stockPolicy.getLowStockThreshold(),
                stockPolicy.getReorderPoint(),
                stockPolicy.getReorderQuantity()
        );
    }

    private String exportStockValuationCsv(StockValuationReportResponse report) {
        StringBuilder builder = new StringBuilder("productId,productSku,productName,warehouseId,warehouseCode,binId,binCode,quantityAvailable,unitPrice,stockValue\n");
        report.items().forEach(item -> builder.append(csv(item.productId())).append(',')
                .append(csv(item.productSku())).append(',')
                .append(csv(item.productName())).append(',')
                .append(csv(item.warehouseId())).append(',')
                .append(csv(item.warehouseCode())).append(',')
                .append(csv(item.binId())).append(',')
                .append(csv(item.binCode())).append(',')
                .append(item.quantityAvailable()).append(',')
                .append(item.unitPrice()).append(',')
                .append(item.stockValue()).append('\n'));
        builder.append("TOTAL,,,,,,,,,").append(report.totalStockValue()).append('\n');
        return builder.toString();
    }

    private String exportLowStockCsv(List<LowStockReportItemResponse> items) {
        StringBuilder builder = new StringBuilder("productId,productSku,productName,warehouseId,warehouseCode,quantityAvailable,lowStockThreshold,reorderPoint,reorderQuantity\n");
        items.forEach(item -> builder.append(csv(item.productId())).append(',')
                .append(csv(item.productSku())).append(',')
                .append(csv(item.productName())).append(',')
                .append(csv(item.warehouseId())).append(',')
                .append(csv(item.warehouseCode())).append(',')
                .append(item.quantityAvailable()).append(',')
                .append(item.lowStockThreshold()).append(',')
                .append(item.reorderPoint()).append(',')
                .append(item.reorderQuantity()).append('\n'));
        return builder.toString();
    }

    private String exportTurnoverCsv(InventoryTurnoverReportResponse report) {
        StringBuilder builder = new StringBuilder("productId,productSku,productName,outboundQuantity,currentAvailable,turnoverRatio\n");
        report.items().forEach(item -> builder.append(csv(item.productId())).append(',')
                .append(csv(item.productSku())).append(',')
                .append(csv(item.productName())).append(',')
                .append(item.outboundQuantity()).append(',')
                .append(item.currentAvailable()).append(',')
                .append(item.turnoverRatio()).append('\n'));
        return builder.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
