package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.inventory.domain.SerialNumber;
import com.noura.platform.inventory.dto.serial.SerialNumberFilter;
import com.noura.platform.inventory.dto.serial.SerialNumberResponse;
import com.noura.platform.inventory.repository.SerialNumberRepository;
import com.noura.platform.inventory.service.SerialTrackingService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SerialTrackingServiceImpl implements SerialTrackingService {

    private final SerialNumberRepository serialNumberRepository;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public Page<SerialNumberResponse> listSerials(SerialNumberFilter filter, Pageable pageable) {
        SerialNumberFilter effectiveFilter = filter == null ? new SerialNumberFilter(null, null, null, null, null, null) : filter;
        return serialNumberRepository.findAll((root, query, cb) -> {
            query.distinct(true);
            java.util.List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (StringUtils.hasText(effectiveFilter.query())) {
                String likeValue = "%" + effectiveFilter.query().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("serialNumber")), likeValue));
            }
            if (StringUtils.hasText(effectiveFilter.productId())) {
                predicates.add(cb.equal(root.join("product").get("id"), effectiveFilter.productId()));
            }
            if (StringUtils.hasText(effectiveFilter.serialStatus())) {
                predicates.add(cb.equal(cb.upper(root.get("serialStatus")), effectiveFilter.serialStatus().trim().toUpperCase(Locale.ROOT)));
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
        }, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public SerialNumberResponse getSerial(String serialId) {
        SerialNumber serial = serialNumberRepository.findByIdAndDeletedAtIsNull(serialId)
                .orElseThrow(() -> new NotFoundException("SERIAL_NOT_FOUND", "Serial number not found"));
        return toResponse(serial);
    }

    private SerialNumberResponse toResponse(SerialNumber serial) {
        return new SerialNumberResponse(
                serial.getId(),
                serial.getProduct().getId(),
                serial.getProduct().getSku(),
                serial.getProduct().getName(),
                serial.getBatch() != null ? serial.getBatch().getId() : null,
                serial.getBatch() != null ? serial.getBatch().getLotNumber() : null,
                serial.getWarehouse() != null ? serial.getWarehouse().getId() : null,
                serial.getWarehouse() != null ? serial.getWarehouse().getWarehouseCode() : null,
                serial.getBin() != null ? serial.getBin().getId() : null,
                serial.getBin() != null ? serial.getBin().getBinCode() : null,
                serial.getSerialNumber(),
                serial.getSerialStatus(),
                serial.getLastMovementLine() != null ? serial.getLastMovementLine().getId() : null,
                serial.getUpdatedAt()
        );
    }
}
