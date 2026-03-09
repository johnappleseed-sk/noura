package com.noura.platform.inventory.service.impl;

import com.noura.platform.inventory.domain.StockLevel;
import com.noura.platform.inventory.domain.StockPolicy;
import com.noura.platform.inventory.dto.stock.StockLevelFilter;
import com.noura.platform.inventory.dto.stock.StockLevelResponse;
import com.noura.platform.inventory.repository.StockLevelRepository;
import com.noura.platform.inventory.repository.StockPolicyRepository;
import com.noura.platform.inventory.service.StockLevelService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockLevelServiceImpl implements StockLevelService {

    private final StockLevelRepository stockLevelRepository;
    private final StockPolicyRepository stockPolicyRepository;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public Page<StockLevelResponse> listStockLevels(StockLevelFilter filter, Pageable pageable) {
        StockLevelFilter effectiveFilter = filter == null
                ? new StockLevelFilter(null, null, null, null, false)
                : filter;
        var specification = (org.springframework.data.jpa.domain.Specification<StockLevel>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(effectiveFilter.productId())) {
                predicates.add(cb.equal(root.join("product").get("id"), effectiveFilter.productId()));
            }
            if (StringUtils.hasText(effectiveFilter.warehouseId())) {
                predicates.add(cb.equal(root.join("warehouse").get("id"), effectiveFilter.warehouseId()));
            }
            if (StringUtils.hasText(effectiveFilter.binId())) {
                predicates.add(cb.equal(root.join("bin").get("id"), effectiveFilter.binId()));
            }
            if (StringUtils.hasText(effectiveFilter.batchId())) {
                predicates.add(cb.equal(root.join("batch").get("id"), effectiveFilter.batchId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        if (Boolean.TRUE.equals(effectiveFilter.lowStockOnly())) {
            List<StockLevelResponse> filtered = stockLevelRepository.findAll(specification).stream()
                    .map(this::toResponse)
                    .filter(StockLevelResponse::lowStock)
                    .toList();
            int start = Math.min((int) pageable.getOffset(), filtered.size());
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
        }

        return stockLevelRepository.findAll(specification, pageable).map(this::toResponse);
    }

    private StockLevelResponse toResponse(StockLevel stockLevel) {
        StockPolicy stockPolicy = stockPolicyRepository.findByProduct_IdAndWarehouse_Id(
                        stockLevel.getProduct().getId(),
                        stockLevel.getWarehouse().getId()
                )
                .or(() -> stockPolicyRepository.findByProduct_IdAndWarehouseIsNull(stockLevel.getProduct().getId()))
                .orElse(null);
        BigDecimal threshold = stockPolicy != null && stockPolicy.getLowStockThreshold() != null
                ? stockPolicy.getLowStockThreshold()
                : BigDecimal.ZERO;
        boolean lowStock = threshold.signum() > 0 && stockLevel.getQuantityAvailable().compareTo(threshold) <= 0;
        return new StockLevelResponse(
                stockLevel.getId(),
                stockLevel.getProduct().getId(),
                stockLevel.getProduct().getSku(),
                stockLevel.getProduct().getName(),
                stockLevel.getWarehouse().getId(),
                stockLevel.getWarehouse().getWarehouseCode(),
                stockLevel.getWarehouse().getName(),
                stockLevel.getBin() != null ? stockLevel.getBin().getId() : null,
                stockLevel.getBin() != null ? stockLevel.getBin().getBinCode() : null,
                stockLevel.getBatch() != null ? stockLevel.getBatch().getId() : null,
                stockLevel.getBatch() != null ? stockLevel.getBatch().getLotNumber() : null,
                stockLevel.getQuantityOnHand(),
                stockLevel.getQuantityReserved(),
                stockLevel.getQuantityAvailable(),
                stockLevel.getQuantityDamaged(),
                stockLevel.getLastMovementAt(),
                lowStock,
                threshold,
                stockLevel.getUpdatedAt()
        );
    }
}
