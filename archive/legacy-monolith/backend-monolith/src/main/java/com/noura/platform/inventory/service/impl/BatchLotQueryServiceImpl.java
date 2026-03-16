package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.inventory.domain.BatchLot;
import com.noura.platform.inventory.dto.batch.BatchLotFilter;
import com.noura.platform.inventory.dto.batch.BatchLotResponse;
import com.noura.platform.inventory.repository.BatchLotRepository;
import com.noura.platform.inventory.repository.StockLevelRepository;
import com.noura.platform.inventory.service.BatchLotQueryService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class BatchLotQueryServiceImpl implements BatchLotQueryService {

    private final BatchLotRepository batchLotRepository;
    private final StockLevelRepository stockLevelRepository;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public Page<BatchLotResponse> listBatches(BatchLotFilter filter, Pageable pageable) {
        BatchLotFilter effectiveFilter = filter == null ? new BatchLotFilter(null, null, null, null) : filter;
        return batchLotRepository.findAll((root, query, cb) -> {
            query.distinct(true);
            java.util.List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (StringUtils.hasText(effectiveFilter.productId())) {
                predicates.add(cb.equal(root.join("product").get("id"), effectiveFilter.productId()));
            }
            if (StringUtils.hasText(effectiveFilter.status())) {
                predicates.add(cb.equal(cb.upper(root.get("status")), effectiveFilter.status().trim().toUpperCase()));
            }
            if (effectiveFilter.expiringBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expiryDate"), effectiveFilter.expiringBefore()));
            }
            if (effectiveFilter.expiringAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expiryDate"), effectiveFilter.expiringAfter()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public BatchLotResponse getBatch(String batchId) {
        BatchLot batch = batchLotRepository.findByIdAndDeletedAtIsNull(batchId)
                .orElseThrow(() -> new NotFoundException("BATCH_NOT_FOUND", "Batch not found"));
        return toResponse(batch);
    }

    private BatchLotResponse toResponse(BatchLot batch) {
        return new BatchLotResponse(
                batch.getId(),
                batch.getProduct().getId(),
                batch.getProduct().getSku(),
                batch.getProduct().getName(),
                batch.getLotNumber(),
                batch.getSupplierBatchRef(),
                batch.getManufacturedAt(),
                batch.getReceivedAt(),
                batch.getExpiryDate(),
                batch.getStatus(),
                batch.getNotes(),
                stockLevelRepository.sumOnHandByBatch(batch.getId()),
                stockLevelRepository.sumAvailableByBatch(batch.getId())
        );
    }
}
