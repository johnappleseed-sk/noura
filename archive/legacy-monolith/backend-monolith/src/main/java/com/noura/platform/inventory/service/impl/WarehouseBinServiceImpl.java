package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.ApiException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.inventory.domain.Warehouse;
import com.noura.platform.inventory.domain.WarehouseBin;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinFilter;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinRequest;
import com.noura.platform.inventory.dto.warehouse.WarehouseBinResponse;
import com.noura.platform.inventory.mapper.WarehouseBinMapper;
import com.noura.platform.inventory.repository.WarehouseBinRepository;
import com.noura.platform.inventory.repository.InventoryWarehouseRepository;
import com.noura.platform.inventory.service.WarehouseBinService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseBinServiceImpl implements WarehouseBinService {

    private final WarehouseBinRepository warehouseBinRepository;
    private final InventoryWarehouseRepository warehouseRepository;
    private final WarehouseBinMapper warehouseBinMapper;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public WarehouseBinResponse createBin(String warehouseId, WarehouseBinRequest request) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        validateUniqueBinCode(warehouseId, request.binCode(), null);
        WarehouseBin bin = new WarehouseBin();
        bin.setWarehouse(warehouse);
        applyBin(bin, request);
        return warehouseBinMapper.toResponse(warehouseBinRepository.save(bin));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public WarehouseBinResponse updateBin(String binId, WarehouseBinRequest request) {
        WarehouseBin bin = getBinEntity(binId);
        validateUniqueBinCode(bin.getWarehouse().getId(), request.binCode(), binId);
        applyBin(bin, request);
        return warehouseBinMapper.toResponse(warehouseBinRepository.save(bin));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public WarehouseBinResponse getBin(String binId) {
        return warehouseBinMapper.toResponse(getBinEntity(binId));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public Page<WarehouseBinResponse> listBins(WarehouseBinFilter filter, Pageable pageable) {
        WarehouseBinFilter effectiveFilter = filter == null ? new WarehouseBinFilter(null, null, null, null) : filter;
        return warehouseBinRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (StringUtils.hasText(effectiveFilter.warehouseId())) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), effectiveFilter.warehouseId()));
            }
            if (StringUtils.hasText(effectiveFilter.zoneCode())) {
                predicates.add(cb.equal(cb.lower(root.get("zoneCode")), effectiveFilter.zoneCode().trim().toLowerCase()));
            }
            if (StringUtils.hasText(effectiveFilter.query())) {
                String likeValue = "%" + effectiveFilter.query().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("binCode")), likeValue),
                        cb.like(cb.lower(root.get("zoneCode")), likeValue),
                        cb.like(cb.lower(root.get("aisleCode")), likeValue),
                        cb.like(cb.lower(root.get("shelfCode")), likeValue)
                ));
            }
            if (effectiveFilter.active() != null) {
                predicates.add(cb.equal(root.get("active"), effectiveFilter.active()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(warehouseBinMapper::toResponse);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public void deleteBin(String binId) {
        WarehouseBin bin = getBinEntity(binId);
        bin.setActive(false);
        bin.setDeletedAt(Instant.now());
        warehouseBinRepository.save(bin);
    }

    private Warehouse getWarehouseEntity(String warehouseId) {
        return warehouseRepository.findByIdAndDeletedAtIsNull(warehouseId)
                .orElseThrow(() -> new NotFoundException("WAREHOUSE_NOT_FOUND", "Warehouse not found"));
    }

    private WarehouseBin getBinEntity(String binId) {
        return warehouseBinRepository.findByIdAndDeletedAtIsNull(binId)
                .orElseThrow(() -> new NotFoundException("WAREHOUSE_BIN_NOT_FOUND", "Warehouse bin not found"));
    }

    private void validateUniqueBinCode(String warehouseId, String binCode, String binId) {
        boolean exists = binId == null
                ? warehouseBinRepository.existsByWarehouse_IdAndBinCodeIgnoreCaseAndDeletedAtIsNull(warehouseId, binCode)
                : warehouseBinRepository.existsByWarehouse_IdAndBinCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(warehouseId, binCode, binId);
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "WAREHOUSE_BIN_CODE_EXISTS", "Bin code already exists in this warehouse");
        }
    }

    private void applyBin(WarehouseBin bin, WarehouseBinRequest request) {
        bin.setBinCode(request.binCode().trim());
        bin.setZoneCode(normalizeNullable(request.zoneCode()));
        bin.setAisleCode(normalizeNullable(request.aisleCode()));
        bin.setShelfCode(normalizeNullable(request.shelfCode()));
        bin.setBinType(request.binType().trim().toUpperCase());
        bin.setBarcodeValue(normalizeNullable(request.barcodeValue()));
        bin.setQrCodeValue(normalizeNullable(request.qrCodeValue()));
        bin.setPickSequence(request.pickSequence() == null ? 0 : request.pickSequence());
        bin.setActive(request.active() == null || request.active());
        if (bin.isActive()) {
            bin.setDeletedAt(null);
        }
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
