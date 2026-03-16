package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.ApiException;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.inventory.domain.Warehouse;
import com.noura.platform.inventory.dto.warehouse.WarehouseFilter;
import com.noura.platform.inventory.dto.warehouse.WarehouseRequest;
import com.noura.platform.inventory.dto.warehouse.WarehouseResponse;
import com.noura.platform.inventory.mapper.WarehouseMapper;
import com.noura.platform.inventory.repository.WarehouseBinRepository;
import com.noura.platform.inventory.repository.InventoryWarehouseRepository;
import com.noura.platform.inventory.service.WarehouseService;
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
public class WarehouseServiceImpl implements WarehouseService {

    private final InventoryWarehouseRepository warehouseRepository;
    private final WarehouseBinRepository warehouseBinRepository;
    private final WarehouseMapper warehouseMapper;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        validateUniqueCode(request.warehouseCode(), null);
        Warehouse warehouse = new Warehouse();
        applyWarehouse(warehouse, request);
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public WarehouseResponse updateWarehouse(String warehouseId, WarehouseRequest request) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        validateUniqueCode(request.warehouseCode(), warehouseId);
        applyWarehouse(warehouse, request);
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public WarehouseResponse getWarehouse(String warehouseId) {
        return warehouseMapper.toResponse(getWarehouseEntity(warehouseId));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public Page<WarehouseResponse> listWarehouses(WarehouseFilter filter, Pageable pageable) {
        WarehouseFilter effectiveFilter = filter == null ? new WarehouseFilter(null, null) : filter;
        return warehouseRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (StringUtils.hasText(effectiveFilter.query())) {
                String likeValue = "%" + effectiveFilter.query().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likeValue),
                        cb.like(cb.lower(root.get("warehouseCode")), likeValue),
                        cb.like(cb.lower(root.get("city")), likeValue)
                ));
            }
            if (effectiveFilter.active() != null) {
                predicates.add(cb.equal(root.get("active"), effectiveFilter.active()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(warehouseMapper::toResponse);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public void deleteWarehouse(String warehouseId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        if (warehouseBinRepository.existsByWarehouse_IdAndDeletedAtIsNull(warehouseId)) {
            throw new BadRequestException("WAREHOUSE_HAS_BINS", "Cannot delete a warehouse that still contains active bins");
        }
        warehouse.setActive(false);
        warehouse.setDeletedAt(Instant.now());
        warehouseRepository.save(warehouse);
    }

    private Warehouse getWarehouseEntity(String warehouseId) {
        return warehouseRepository.findByIdAndDeletedAtIsNull(warehouseId)
                .orElseThrow(() -> new NotFoundException("WAREHOUSE_NOT_FOUND", "Warehouse not found"));
    }

    private void validateUniqueCode(String warehouseCode, String warehouseId) {
        boolean exists = warehouseId == null
                ? warehouseRepository.existsByWarehouseCodeIgnoreCaseAndDeletedAtIsNull(warehouseCode)
                : warehouseRepository.existsByWarehouseCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(warehouseCode, warehouseId);
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "WAREHOUSE_CODE_EXISTS", "Warehouse code already exists");
        }
    }

    private void applyWarehouse(Warehouse warehouse, WarehouseRequest request) {
        warehouse.setWarehouseCode(request.warehouseCode().trim());
        warehouse.setName(request.name().trim());
        warehouse.setWarehouseType(request.warehouseType().trim().toUpperCase());
        warehouse.setAddressLine1(normalizeNullable(request.addressLine1()));
        warehouse.setAddressLine2(normalizeNullable(request.addressLine2()));
        warehouse.setCity(normalizeNullable(request.city()));
        warehouse.setStateProvince(normalizeNullable(request.stateProvince()));
        warehouse.setPostalCode(normalizeNullable(request.postalCode()));
        warehouse.setCountryCode(StringUtils.hasText(request.countryCode()) ? request.countryCode().trim().toUpperCase() : null);
        warehouse.setActive(request.active() == null || request.active());
        if (warehouse.isActive()) {
            warehouse.setDeletedAt(null);
        }
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
