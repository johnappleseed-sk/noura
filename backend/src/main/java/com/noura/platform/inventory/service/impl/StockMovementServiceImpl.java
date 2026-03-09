package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.ApiException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.inventory.domain.BatchLot;
import com.noura.platform.inventory.domain.IamUser;
import com.noura.platform.inventory.domain.Product;
import com.noura.platform.inventory.domain.ReorderAlert;
import com.noura.platform.inventory.domain.SerialNumber;
import com.noura.platform.inventory.domain.StockLevel;
import com.noura.platform.inventory.domain.StockMovement;
import com.noura.platform.inventory.domain.StockMovementLine;
import com.noura.platform.inventory.domain.StockPolicy;
import com.noura.platform.inventory.domain.Warehouse;
import com.noura.platform.inventory.domain.WarehouseBin;
import com.noura.platform.inventory.dto.stock.AdjustmentMovementLineRequest;
import com.noura.platform.inventory.dto.stock.AdjustmentMovementRequest;
import com.noura.platform.inventory.dto.stock.InboundMovementRequest;
import com.noura.platform.inventory.dto.stock.OutboundMovementRequest;
import com.noura.platform.inventory.dto.stock.ReturnMovementRequest;
import com.noura.platform.inventory.dto.stock.StockMovementFilter;
import com.noura.platform.inventory.dto.stock.StockMovementLineRequest;
import com.noura.platform.inventory.dto.stock.StockMovementLineResponse;
import com.noura.platform.inventory.dto.stock.StockMovementResponse;
import com.noura.platform.inventory.dto.stock.TransferMovementRequest;
import com.noura.platform.inventory.repository.BatchLotRepository;
import com.noura.platform.inventory.repository.IamUserRepository;
import com.noura.platform.inventory.repository.InventoryProductRepository;
import com.noura.platform.inventory.repository.ReorderAlertRepository;
import com.noura.platform.inventory.repository.SerialNumberRepository;
import com.noura.platform.inventory.repository.StockLevelRepository;
import com.noura.platform.inventory.repository.StockMovementLineRepository;
import com.noura.platform.inventory.repository.StockMovementRepository;
import com.noura.platform.inventory.repository.StockPolicyRepository;
import com.noura.platform.inventory.repository.WarehouseBinRepository;
import com.noura.platform.inventory.repository.InventoryWarehouseRepository;
import com.noura.platform.inventory.security.InventorySecurityContext;
import com.noura.platform.inventory.service.StockMovementService;
import com.noura.platform.inventory.webhook.InventoryWebhookPublisher;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class StockMovementServiceImpl implements StockMovementService {

    private static final String MOVEMENT_TYPE_INBOUND = "INBOUND";
    private static final String MOVEMENT_TYPE_OUTBOUND = "OUTBOUND";
    private static final String MOVEMENT_TYPE_RETURN = "RETURN";
    private static final String MOVEMENT_TYPE_TRANSFER = "TRANSFER";
    private static final String MOVEMENT_TYPE_ADJUSTMENT = "ADJUSTMENT";
    private static final String MOVEMENT_STATUS_COMPLETED = "COMPLETED";
    private static final String SERIAL_STATUS_IN_STOCK = "IN_STOCK";
    private static final String SERIAL_STATUS_SOLD = "SOLD";
    private static final String SERIAL_STATUS_ADJUSTED_OUT = "ADJUSTED_OUT";
    private static final String SERIAL_STATUS_RETURNED = "RETURNED";
    private static final String ALERT_STATUS_OPEN = "OPEN";
    private static final String ALERT_STATUS_RESOLVED = "RESOLVED";

    private final InventoryProductRepository productRepository;
    private final InventoryWarehouseRepository warehouseRepository;
    private final WarehouseBinRepository warehouseBinRepository;
    private final BatchLotRepository batchLotRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementLineRepository stockMovementLineRepository;
    private final StockPolicyRepository stockPolicyRepository;
    private final ReorderAlertRepository reorderAlertRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final IamUserRepository iamUserRepository;
    private final InventoryWebhookPublisher inventoryWebhookPublisher;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public StockMovementResponse receiveInbound(InboundMovementRequest request) {
        Warehouse warehouse = requireWarehouse(request.warehouseId());
        WarehouseBin defaultBin = requireBin(warehouse, request.binId(), "binId", false);
        StockMovement movement = startMovement(
                MOVEMENT_TYPE_INBOUND,
                null,
                null,
                warehouse,
                defaultBin,
                request.referenceType(),
                request.referenceId(),
                request.externalReference(),
                request.notes()
        );
        AtomicInteger lineNumber = new AtomicInteger(1);
        List<StockMovementLine> lines = new ArrayList<>();
        Set<ProductWarehouseKey> affected = new LinkedHashSet<>();
        for (StockMovementLineRequest lineRequest : request.lines()) {
            Product product = requireProduct(lineRequest.productId());
            BigDecimal quantity = requirePositiveQuantity(lineRequest.quantity(), "quantity");
            WarehouseBin targetBin = resolveDestinationBin(warehouse, defaultBin, lineRequest.toBinId());
            BatchLot batch = resolveInboundBatch(product, lineRequest);
            validateInboundSerials(product, quantity, lineRequest.serialNumbers());

            StockLevel stockLevel = getOrCreateStockLevel(product, warehouse, targetBin, batch);
            increaseStock(stockLevel, quantity);

            StockMovementLine movementLine = saveMovementLine(
                    movement,
                    lineNumber.getAndIncrement(),
                    product,
                    batch,
                    null,
                    targetBin,
                    quantity,
                    lineRequest.unitCost(),
                    batch != null ? batch.getExpiryDate() : lineRequest.expiryDate(),
                    lineRequest.notes()
            );
            lines.add(movementLine);
            createInboundSerials(product, warehouse, targetBin, batch, movementLine, lineRequest.serialNumbers());
            affected.add(new ProductWarehouseKey(product.getId(), warehouse.getId()));
        }
        completeMovement(movement);
        reconcileAlerts(affected);
        publishMovementEvent(movement, lines);
        return toMovementResponse(movement, lines);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public StockMovementResponse shipOutbound(OutboundMovementRequest request) {
        Warehouse warehouse = requireWarehouse(request.warehouseId());
        WarehouseBin defaultBin = requireBin(warehouse, request.binId(), "binId", false);
        StockMovement movement = startMovement(
                MOVEMENT_TYPE_OUTBOUND,
                warehouse,
                defaultBin,
                null,
                null,
                request.referenceType(),
                request.referenceId(),
                request.externalReference(),
                request.notes()
        );
        AtomicInteger lineNumber = new AtomicInteger(1);
        List<StockMovementLine> lines = new ArrayList<>();
        Set<ProductWarehouseKey> affected = new LinkedHashSet<>();
        for (StockMovementLineRequest lineRequest : request.lines()) {
            Product product = requireProduct(lineRequest.productId());
            BigDecimal quantity = requirePositiveQuantity(lineRequest.quantity(), "quantity");
            WarehouseBin sourceBin = resolveSourceBin(warehouse, defaultBin, lineRequest.fromBinId());
            if (product.isSerialTracked()) {
                List<SerialNumber> serials = allocateSerials(product, warehouse, sourceBin, lineRequest, quantity);
                for (SerialNumber serial : serials) {
                    BatchLot batch = serial.getBatch();
                    WarehouseBin serialBin = serial.getBin() != null ? serial.getBin() : sourceBin;
                    decreaseStock(getOrCreateStockLevel(product, warehouse, serialBin, batch), BigDecimal.ONE);
                    StockMovementLine movementLine = saveMovementLine(
                            movement,
                            lineNumber.getAndIncrement(),
                            product,
                            batch,
                            serialBin,
                            null,
                            BigDecimal.ONE,
                            lineRequest.unitCost(),
                            batch != null ? batch.getExpiryDate() : lineRequest.expiryDate(),
                            lineRequest.notes()
                    );
                    serial.setSerialStatus(SERIAL_STATUS_SOLD);
                    serial.setWarehouse(null);
                    serial.setBin(null);
                    serial.setLastMovementLine(movementLine);
                    serialNumberRepository.save(serial);
                    lines.add(movementLine);
                }
            } else {
                BatchLot batchConstraint = resolveExistingBatchConstraint(product, lineRequest.batchId(), lineRequest.lotNumber());
                BigDecimal remaining = quantity;
                List<StockLevel> candidates = stockLevelRepository.findAllocatableLevels(
                        product.getId(),
                        warehouse.getId(),
                        sourceBin != null ? sourceBin.getId() : null,
                        batchConstraint != null ? batchConstraint.getId() : null
                );
                for (StockLevel candidate : candidates) {
                    if (remaining.signum() == 0) {
                        break;
                    }
                    BigDecimal allocatable = remaining.min(candidate.getQuantityAvailable());
                    if (allocatable.signum() == 0) {
                        continue;
                    }
                    decreaseStock(candidate, allocatable);
                    StockMovementLine movementLine = saveMovementLine(
                            movement,
                            lineNumber.getAndIncrement(),
                            product,
                            candidate.getBatch(),
                            candidate.getBin(),
                            null,
                            allocatable,
                            lineRequest.unitCost(),
                            candidate.getBatch() != null ? candidate.getBatch().getExpiryDate() : lineRequest.expiryDate(),
                            lineRequest.notes()
                    );
                    lines.add(movementLine);
                    remaining = remaining.subtract(allocatable);
                }
                ensureFullyAllocated(remaining, product);
            }
            affected.add(new ProductWarehouseKey(product.getId(), warehouse.getId()));
        }
        completeMovement(movement);
        reconcileAlerts(affected);
        publishMovementEvent(movement, lines);
        return toMovementResponse(movement, lines);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public StockMovementResponse returnStock(ReturnMovementRequest request) {
        Warehouse warehouse = requireWarehouse(request.warehouseId());
        WarehouseBin defaultBin = requireBin(warehouse, request.binId(), "binId", false);
        StockMovement movement = startMovement(
                MOVEMENT_TYPE_RETURN,
                null,
                null,
                warehouse,
                defaultBin,
                request.referenceType(),
                request.referenceId(),
                request.externalReference(),
                request.notes()
        );
        AtomicInteger lineNumber = new AtomicInteger(1);
        List<StockMovementLine> lines = new ArrayList<>();
        Set<ProductWarehouseKey> affected = new LinkedHashSet<>();
        for (StockMovementLineRequest lineRequest : request.lines()) {
            Product product = requireProduct(lineRequest.productId());
            BigDecimal quantity = requirePositiveQuantity(lineRequest.quantity(), "quantity");
            WarehouseBin targetBin = resolveDestinationBin(warehouse, defaultBin, lineRequest.toBinId());
            if (product.isSerialTracked()) {
                validateInboundSerials(product, quantity, lineRequest.serialNumbers());
                for (String rawSerial : lineRequest.serialNumbers()) {
                    String normalized = rawSerial.trim();
                    SerialNumber serial = serialNumberRepository.findBySerialNumberAndDeletedAtIsNull(normalized).orElse(null);
                    BatchLot batch = resolveReturnBatch(product, lineRequest, serial);
                    increaseStock(getOrCreateStockLevel(product, warehouse, targetBin, batch), BigDecimal.ONE);
                    StockMovementLine movementLine = saveMovementLine(
                            movement,
                            lineNumber.getAndIncrement(),
                            product,
                            batch,
                            null,
                            targetBin,
                            BigDecimal.ONE,
                            lineRequest.unitCost(),
                            batch != null ? batch.getExpiryDate() : lineRequest.expiryDate(),
                            lineRequest.notes()
                    );
                    if (serial == null) {
                        serial = new SerialNumber();
                        serial.setProduct(product);
                        serial.setSerialNumber(normalized);
                    } else if (!serial.getProduct().getId().equals(product.getId())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "SERIAL_PRODUCT_MISMATCH", "Serial does not belong to requested product");
                    }
                    serial.setBatch(batch);
                    serial.setWarehouse(warehouse);
                    serial.setBin(targetBin);
                    serial.setSerialStatus(SERIAL_STATUS_RETURNED);
                    serial.setLastMovementLine(movementLine);
                    serialNumberRepository.save(serial);
                    lines.add(movementLine);
                }
            } else {
                BatchLot batch = resolveInboundBatch(product, lineRequest);
                increaseStock(getOrCreateStockLevel(product, warehouse, targetBin, batch), quantity);
                StockMovementLine movementLine = saveMovementLine(
                        movement,
                        lineNumber.getAndIncrement(),
                        product,
                        batch,
                        null,
                        targetBin,
                        quantity,
                        lineRequest.unitCost(),
                        batch != null ? batch.getExpiryDate() : lineRequest.expiryDate(),
                        lineRequest.notes()
                );
                lines.add(movementLine);
            }
            affected.add(new ProductWarehouseKey(product.getId(), warehouse.getId()));
        }
        completeMovement(movement);
        reconcileAlerts(affected);
        publishMovementEvent(movement, lines);
        return toMovementResponse(movement, lines);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public StockMovementResponse transferStock(TransferMovementRequest request) {
        Warehouse sourceWarehouse = requireWarehouse(request.sourceWarehouseId());
        Warehouse destinationWarehouse = requireWarehouse(request.destinationWarehouseId());
        WarehouseBin defaultSourceBin = requireBin(sourceWarehouse, request.sourceBinId(), "sourceBinId", false);
        WarehouseBin defaultDestinationBin = requireBin(destinationWarehouse, request.destinationBinId(), "destinationBinId", false);
        if (sourceWarehouse.getId().equals(destinationWarehouse.getId())
                && sameId(defaultSourceBin != null ? defaultSourceBin.getId() : null, defaultDestinationBin != null ? defaultDestinationBin.getId() : null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TRANSFER_SCOPE_INVALID", "Transfer source and destination must differ");
        }

        StockMovement movement = startMovement(
                MOVEMENT_TYPE_TRANSFER,
                sourceWarehouse,
                defaultSourceBin,
                destinationWarehouse,
                defaultDestinationBin,
                request.referenceType(),
                request.referenceId(),
                request.externalReference(),
                request.notes()
        );
        AtomicInteger lineNumber = new AtomicInteger(1);
        List<StockMovementLine> lines = new ArrayList<>();
        Set<ProductWarehouseKey> affected = new LinkedHashSet<>();

        for (StockMovementLineRequest lineRequest : request.lines()) {
            Product product = requireProduct(lineRequest.productId());
            BigDecimal quantity = requirePositiveQuantity(lineRequest.quantity(), "quantity");
            WarehouseBin sourceBin = resolveSourceBin(sourceWarehouse, defaultSourceBin, lineRequest.fromBinId());
            WarehouseBin destinationBin = resolveDestinationBin(destinationWarehouse, defaultDestinationBin, lineRequest.toBinId());
            if (product.isSerialTracked()) {
                List<SerialNumber> serials = allocateSerials(product, sourceWarehouse, sourceBin, lineRequest, quantity);
                for (SerialNumber serial : serials) {
                    BatchLot batch = serial.getBatch();
                    WarehouseBin serialBin = serial.getBin() != null ? serial.getBin() : sourceBin;
                    decreaseStock(getOrCreateStockLevel(product, sourceWarehouse, serialBin, batch), BigDecimal.ONE);
                    increaseStock(getOrCreateStockLevel(product, destinationWarehouse, destinationBin, batch), BigDecimal.ONE);
                    StockMovementLine movementLine = saveMovementLine(
                            movement,
                            lineNumber.getAndIncrement(),
                            product,
                            batch,
                            serialBin,
                            destinationBin,
                            BigDecimal.ONE,
                            lineRequest.unitCost(),
                            batch != null ? batch.getExpiryDate() : lineRequest.expiryDate(),
                            lineRequest.notes()
                    );
                    serial.setWarehouse(destinationWarehouse);
                    serial.setBin(destinationBin);
                    serial.setLastMovementLine(movementLine);
                    serialNumberRepository.save(serial);
                    lines.add(movementLine);
                }
            } else {
                BatchLot batchConstraint = resolveExistingBatchConstraint(product, lineRequest.batchId(), lineRequest.lotNumber());
                BigDecimal remaining = quantity;
                List<StockLevel> candidates = stockLevelRepository.findAllocatableLevels(
                        product.getId(),
                        sourceWarehouse.getId(),
                        sourceBin != null ? sourceBin.getId() : null,
                        batchConstraint != null ? batchConstraint.getId() : null
                );
                for (StockLevel candidate : candidates) {
                    if (remaining.signum() == 0) {
                        break;
                    }
                    BigDecimal allocatable = remaining.min(candidate.getQuantityAvailable());
                    if (allocatable.signum() == 0) {
                        continue;
                    }
                    decreaseStock(candidate, allocatable);
                    increaseStock(getOrCreateStockLevel(product, destinationWarehouse, destinationBin, candidate.getBatch()), allocatable);
                    StockMovementLine movementLine = saveMovementLine(
                            movement,
                            lineNumber.getAndIncrement(),
                            product,
                            candidate.getBatch(),
                            candidate.getBin(),
                            destinationBin,
                            allocatable,
                            lineRequest.unitCost(),
                            candidate.getBatch() != null ? candidate.getBatch().getExpiryDate() : lineRequest.expiryDate(),
                            lineRequest.notes()
                    );
                    lines.add(movementLine);
                    remaining = remaining.subtract(allocatable);
                }
                ensureFullyAllocated(remaining, product);
            }
            affected.add(new ProductWarehouseKey(product.getId(), sourceWarehouse.getId()));
            affected.add(new ProductWarehouseKey(product.getId(), destinationWarehouse.getId()));
        }

        completeMovement(movement);
        reconcileAlerts(affected);
        publishMovementEvent(movement, lines);
        return toMovementResponse(movement, lines);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public StockMovementResponse adjustStock(AdjustmentMovementRequest request) {
        Warehouse warehouse = requireWarehouse(request.warehouseId());
        WarehouseBin defaultBin = requireBin(warehouse, request.binId(), "binId", false);
        StockMovement movement = startMovement(
                MOVEMENT_TYPE_ADJUSTMENT,
                warehouse,
                defaultBin,
                null,
                null,
                StringUtils.hasText(request.reasonCode()) ? "ADJUSTMENT:" + request.reasonCode().trim().toUpperCase(Locale.ROOT) : "ADJUSTMENT",
                request.referenceId(),
                request.externalReference(),
                request.notes()
        );
        AtomicInteger lineNumber = new AtomicInteger(1);
        List<StockMovementLine> lines = new ArrayList<>();
        Set<ProductWarehouseKey> affected = new LinkedHashSet<>();

        for (AdjustmentMovementLineRequest lineRequest : request.lines()) {
            Product product = requireProduct(lineRequest.productId());
            BigDecimal quantityDelta = requireNonZeroQuantity(lineRequest.quantityDelta(), "quantityDelta");
            WarehouseBin bin = resolveAdjustmentBin(warehouse, defaultBin, lineRequest.binId());
            if (quantityDelta.signum() > 0) {
                BatchLot batch = resolveInboundBatch(product, lineRequest);
                validateInboundSerials(product, quantityDelta, lineRequest.serialNumbers());
                increaseStock(getOrCreateStockLevel(product, warehouse, bin, batch), quantityDelta);
                StockMovementLine movementLine = saveMovementLine(
                        movement,
                        lineNumber.getAndIncrement(),
                        product,
                        batch,
                        null,
                        bin,
                        quantityDelta,
                        lineRequest.unitCost(),
                        batch != null ? batch.getExpiryDate() : lineRequest.expiryDate(),
                        lineRequest.notes()
                );
                createInboundSerials(product, warehouse, bin, batch, movementLine, lineRequest.serialNumbers());
                lines.add(movementLine);
            } else if (product.isSerialTracked()) {
                BigDecimal issueQuantity = quantityDelta.abs();
                List<String> requestedSerials = lineRequest.serialNumbers();
                validateSerialQuantity(product, issueQuantity, requestedSerials, false);
                List<SerialNumber> serials = allocateSerials(product, warehouse, bin, lineRequest.batchId(), lineRequest.lotNumber(), requestedSerials, issueQuantity);
                for (SerialNumber serial : serials) {
                    BatchLot batch = serial.getBatch();
                    WarehouseBin serialBin = serial.getBin() != null ? serial.getBin() : bin;
                    decreaseStock(getOrCreateStockLevel(product, warehouse, serialBin, batch), BigDecimal.ONE);
                    StockMovementLine movementLine = saveMovementLine(
                            movement,
                            lineNumber.getAndIncrement(),
                            product,
                            batch,
                            serialBin,
                            null,
                            BigDecimal.ONE.negate(),
                            lineRequest.unitCost(),
                            batch != null ? batch.getExpiryDate() : lineRequest.expiryDate(),
                            lineRequest.notes()
                    );
                    serial.setSerialStatus(SERIAL_STATUS_ADJUSTED_OUT);
                    serial.setWarehouse(null);
                    serial.setBin(null);
                    serial.setLastMovementLine(movementLine);
                    serialNumberRepository.save(serial);
                    lines.add(movementLine);
                }
            } else {
                BatchLot batchConstraint = resolveExistingBatchConstraint(product, lineRequest.batchId(), lineRequest.lotNumber());
                BigDecimal remaining = quantityDelta.abs();
                List<StockLevel> candidates = stockLevelRepository.findAllocatableLevels(
                        product.getId(),
                        warehouse.getId(),
                        bin != null ? bin.getId() : null,
                        batchConstraint != null ? batchConstraint.getId() : null
                );
                for (StockLevel candidate : candidates) {
                    if (remaining.signum() == 0) {
                        break;
                    }
                    BigDecimal allocatable = remaining.min(candidate.getQuantityAvailable());
                    if (allocatable.signum() == 0) {
                        continue;
                    }
                    decreaseStock(candidate, allocatable);
                    StockMovementLine movementLine = saveMovementLine(
                            movement,
                            lineNumber.getAndIncrement(),
                            product,
                            candidate.getBatch(),
                            candidate.getBin(),
                            null,
                            allocatable.negate(),
                            lineRequest.unitCost(),
                            candidate.getBatch() != null ? candidate.getBatch().getExpiryDate() : lineRequest.expiryDate(),
                            lineRequest.notes()
                    );
                    lines.add(movementLine);
                    remaining = remaining.subtract(allocatable);
                }
                ensureFullyAllocated(remaining, product);
            }
            affected.add(new ProductWarehouseKey(product.getId(), warehouse.getId()));
        }

        completeMovement(movement);
        reconcileAlerts(affected);
        publishMovementEvent(movement, lines);
        return toMovementResponse(movement, lines);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public Page<StockMovementResponse> listMovements(StockMovementFilter filter, Pageable pageable) {
        StockMovementFilter effectiveFilter = filter == null
                ? new StockMovementFilter(null, null, null, null, null, null, null)
                : filter;
        return stockMovementRepository.findAll((root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (StringUtils.hasText(effectiveFilter.movementType())) {
                predicates.add(cb.equal(root.get("movementType"), effectiveFilter.movementType().trim().toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(effectiveFilter.movementStatus())) {
                predicates.add(cb.equal(root.get("movementStatus"), effectiveFilter.movementStatus().trim().toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(effectiveFilter.warehouseId())) {
                predicates.add(cb.or(
                        cb.equal(root.join("sourceWarehouse", JoinType.LEFT).get("id"), effectiveFilter.warehouseId()),
                        cb.equal(root.join("destinationWarehouse", JoinType.LEFT).get("id"), effectiveFilter.warehouseId())
                ));
            }
            if (StringUtils.hasText(effectiveFilter.referenceQuery())) {
                String likeValue = "%" + effectiveFilter.referenceQuery().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("movementNumber")), likeValue),
                        cb.like(cb.lower(root.get("referenceId")), likeValue),
                        cb.like(cb.lower(root.get("externalReference")), likeValue)
                ));
            }
            if (effectiveFilter.processedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("processedAt"), effectiveFilter.processedFrom()));
            }
            if (effectiveFilter.processedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("processedAt"), effectiveFilter.processedTo()));
            }
            if (StringUtils.hasText(effectiveFilter.productId())) {
                var subquery = query.subquery(String.class);
                var lineRoot = subquery.from(StockMovementLine.class);
                subquery.select(lineRoot.get("movement").get("id"));
                subquery.where(
                        cb.equal(lineRoot.get("movement").get("id"), root.get("id")),
                        cb.equal(lineRoot.get("product").get("id"), effectiveFilter.productId())
                );
                predicates.add(cb.exists(subquery));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(movement -> toMovementResponse(
                movement,
                stockMovementLineRepository.findAllDetailedByMovementId(movement.getId())
        ));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public StockMovementResponse getMovement(String movementId) {
        StockMovement movement = stockMovementRepository.findByIdAndDeletedAtIsNull(movementId)
                .orElseThrow(() -> new NotFoundException("MOVEMENT_NOT_FOUND", "Stock movement not found"));
        List<StockMovementLine> lines = stockMovementLineRepository.findAllDetailedByMovementId(movementId);
        return toMovementResponse(movement, lines);
    }

    private StockMovement startMovement(String movementType,
                                        Warehouse sourceWarehouse,
                                        WarehouseBin sourceBin,
                                        Warehouse destinationWarehouse,
                                        WarehouseBin destinationBin,
                                        String referenceType,
                                        String referenceId,
                                        String externalReference,
                                        String notes) {
        StockMovement movement = new StockMovement();
        movement.setMovementNumber(buildMovementNumber(movementType));
        movement.setMovementType(movementType);
        movement.setMovementStatus(MOVEMENT_STATUS_COMPLETED);
        movement.setSourceWarehouse(sourceWarehouse);
        movement.setSourceBin(sourceBin);
        movement.setDestinationWarehouse(destinationWarehouse);
        movement.setDestinationBin(destinationBin);
        movement.setReferenceType(normalizeNullable(referenceType));
        movement.setReferenceId(normalizeNullable(referenceId));
        movement.setExternalReference(normalizeNullable(externalReference));
        movement.setNotes(normalizeNullable(notes));
        movement.setProcessedBy(resolveCurrentUser());
        movement.setProcessedAt(Instant.now());
        return stockMovementRepository.save(movement);
    }

    private void completeMovement(StockMovement movement) {
        movement.setMovementStatus(MOVEMENT_STATUS_COMPLETED);
        movement.setProcessedAt(Instant.now());
        if (movement.getProcessedBy() == null) {
            movement.setProcessedBy(resolveCurrentUser());
        }
        stockMovementRepository.save(movement);
    }

    private StockMovementLine saveMovementLine(StockMovement movement,
                                               int lineNumber,
                                               Product product,
                                               BatchLot batch,
                                               WarehouseBin fromBin,
                                               WarehouseBin toBin,
                                               BigDecimal quantity,
                                               BigDecimal unitCost,
                                               LocalDate expiryDate,
                                               String notes) {
        StockMovementLine line = new StockMovementLine();
        line.setMovement(movement);
        line.setLineNumber(lineNumber);
        line.setProduct(product);
        line.setBatch(batch);
        line.setFromBin(fromBin);
        line.setToBin(toBin);
        line.setQuantity(quantity);
        line.setUnitCost(unitCost);
        line.setExpiryDate(expiryDate);
        line.setNotes(normalizeNullable(notes));
        return stockMovementLineRepository.save(line);
    }

    private Product requireProduct(String productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        if (!product.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PRODUCT_INACTIVE", "Inactive products cannot be moved");
        }
        return product;
    }

    private Warehouse requireWarehouse(String warehouseId) {
        Warehouse warehouse = warehouseRepository.findByIdAndDeletedAtIsNull(warehouseId)
                .orElseThrow(() -> new NotFoundException("WAREHOUSE_NOT_FOUND", "Warehouse not found"));
        if (!warehouse.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WAREHOUSE_INACTIVE", "Inactive warehouses cannot process movements");
        }
        return warehouse;
    }

    private WarehouseBin requireBin(Warehouse warehouse, String binId, String fieldName, boolean mandatory) {
        if (!StringUtils.hasText(binId)) {
            if (mandatory) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "BIN_REQUIRED", fieldName + " is required");
            }
            return null;
        }
        WarehouseBin bin = warehouseBinRepository.findByIdAndDeletedAtIsNull(binId.trim())
                .orElseThrow(() -> new NotFoundException("BIN_NOT_FOUND", "Warehouse bin not found"));
        if (!bin.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BIN_INACTIVE", "Inactive bins cannot process movements");
        }
        if (!bin.getWarehouse().getId().equals(warehouse.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BIN_WAREHOUSE_MISMATCH", fieldName + " does not belong to warehouse " + warehouse.getWarehouseCode());
        }
        return bin;
    }

    private WarehouseBin resolveSourceBin(Warehouse warehouse, WarehouseBin defaultBin, String overrideBinId) {
        return StringUtils.hasText(overrideBinId) ? requireBin(warehouse, overrideBinId, "fromBinId", true) : defaultBin;
    }

    private WarehouseBin resolveDestinationBin(Warehouse warehouse, WarehouseBin defaultBin, String overrideBinId) {
        return StringUtils.hasText(overrideBinId) ? requireBin(warehouse, overrideBinId, "toBinId", true) : defaultBin;
    }

    private WarehouseBin resolveAdjustmentBin(Warehouse warehouse, WarehouseBin defaultBin, String overrideBinId) {
        return StringUtils.hasText(overrideBinId) ? requireBin(warehouse, overrideBinId, "binId", true) : defaultBin;
    }

    private BatchLot resolveInboundBatch(Product product, StockMovementLineRequest request) {
        return resolveInboundBatch(
                product,
                request.batchId(),
                request.lotNumber(),
                request.expiryDate(),
                request.manufacturedAt(),
                request.supplierBatchRef(),
                request.notes()
        );
    }

    private BatchLot resolveReturnBatch(Product product, StockMovementLineRequest request, SerialNumber serial) {
        if ((request.batchId() == null || request.batchId().isBlank())
                && (request.lotNumber() == null || request.lotNumber().isBlank())
                && serial != null
                && serial.getBatch() != null) {
            return serial.getBatch();
        }
        return resolveInboundBatch(product, request);
    }

    private BatchLot resolveInboundBatch(Product product, AdjustmentMovementLineRequest request) {
        return resolveInboundBatch(
                product,
                request.batchId(),
                request.lotNumber(),
                request.expiryDate(),
                request.manufacturedAt(),
                request.supplierBatchRef(),
                request.notes()
        );
    }

    private BatchLot resolveInboundBatch(Product product,
                                         String batchId,
                                         String lotNumber,
                                         LocalDate expiryDate,
                                         LocalDate manufacturedAt,
                                         String supplierBatchRef,
                                         String notes) {
        if (!product.isBatchTracked()) {
            if (StringUtils.hasText(batchId) || StringUtils.hasText(lotNumber)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "BATCH_NOT_ALLOWED", "Batch fields are only allowed for batch-tracked products");
            }
            return null;
        }
        if (!StringUtils.hasText(batchId) && !StringUtils.hasText(lotNumber)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BATCH_REQUIRED", "Batch-tracked products require batchId or lotNumber");
        }
        BatchLot batch = StringUtils.hasText(batchId)
                ? batchLotRepository.findByIdAndDeletedAtIsNull(batchId.trim())
                .orElseThrow(() -> new NotFoundException("BATCH_NOT_FOUND", "Batch not found"))
                : batchLotRepository.findByProduct_IdAndLotNumberIgnoreCaseAndDeletedAtIsNull(product.getId(), lotNumber.trim())
                .orElseGet(() -> createBatchLot(product, lotNumber, expiryDate, manufacturedAt, supplierBatchRef, notes));
        if (!batch.getProduct().getId().equals(product.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BATCH_PRODUCT_MISMATCH", "Batch does not belong to the requested product");
        }
        if (expiryDate != null) {
            batch.setExpiryDate(expiryDate);
        }
        if (manufacturedAt != null) {
            batch.setManufacturedAt(manufacturedAt);
        }
        if (StringUtils.hasText(supplierBatchRef)) {
            batch.setSupplierBatchRef(supplierBatchRef.trim());
        }
        if (StringUtils.hasText(notes)) {
            batch.setNotes(notes.trim());
        }
        if (batch.getReceivedAt() == null) {
            batch.setReceivedAt(Instant.now());
        }
        batch.setStatus("ACTIVE");
        return batchLotRepository.save(batch);
    }

    private BatchLot createBatchLot(Product product,
                                    String lotNumber,
                                    LocalDate expiryDate,
                                    LocalDate manufacturedAt,
                                    String supplierBatchRef,
                                    String notes) {
        BatchLot batch = new BatchLot();
        batch.setProduct(product);
        batch.setLotNumber(lotNumber.trim());
        batch.setExpiryDate(expiryDate);
        batch.setManufacturedAt(manufacturedAt);
        batch.setSupplierBatchRef(normalizeNullable(supplierBatchRef));
        batch.setNotes(normalizeNullable(notes));
        batch.setReceivedAt(Instant.now());
        batch.setStatus("ACTIVE");
        return batchLotRepository.save(batch);
    }

    private BatchLot resolveExistingBatchConstraint(Product product, String batchId, String lotNumber) {
        if (!product.isBatchTracked()) {
            if (StringUtils.hasText(batchId) || StringUtils.hasText(lotNumber)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "BATCH_NOT_ALLOWED", "Batch fields are only allowed for batch-tracked products");
            }
            return null;
        }
        if (!StringUtils.hasText(batchId) && !StringUtils.hasText(lotNumber)) {
            return null;
        }
        BatchLot batch = StringUtils.hasText(batchId)
                ? batchLotRepository.findByIdAndDeletedAtIsNull(batchId.trim())
                .orElseThrow(() -> new NotFoundException("BATCH_NOT_FOUND", "Batch not found"))
                : batchLotRepository.findByProduct_IdAndLotNumberIgnoreCaseAndDeletedAtIsNull(product.getId(), lotNumber.trim())
                .orElseThrow(() -> new NotFoundException("BATCH_NOT_FOUND", "Batch not found"));
        if (!batch.getProduct().getId().equals(product.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BATCH_PRODUCT_MISMATCH", "Batch does not belong to the requested product");
        }
        return batch;
    }

    private void validateInboundSerials(Product product, BigDecimal quantity, List<String> serialNumbers) {
        if (!product.isSerialTracked()) {
            if (serialNumbers != null && !serialNumbers.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SERIAL_NOT_ALLOWED", "Serial numbers are only allowed for serial-tracked products");
            }
            return;
        }
        validateSerialQuantity(product, quantity, serialNumbers, true);
    }

    private void validateSerialQuantity(Product product, BigDecimal quantity, List<String> serialNumbers, boolean requireList) {
        if (!product.isSerialTracked()) {
            return;
        }
        int unitCount = requireWholeNumber(quantity, "quantity");
        if (requireList && (serialNumbers == null || serialNumbers.isEmpty())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SERIALS_REQUIRED", "Serial-tracked products require explicit serial numbers");
        }
        if (serialNumbers != null && !serialNumbers.isEmpty()) {
            if (serialNumbers.size() != unitCount) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SERIAL_COUNT_MISMATCH", "Serial number count must match the moved quantity");
            }
            Set<String> uniqueSerials = new LinkedHashSet<>();
            for (String serial : serialNumbers) {
                if (!StringUtils.hasText(serial)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "SERIAL_INVALID", "Serial numbers cannot be blank");
                }
                String normalized = serial.trim();
                if (!uniqueSerials.add(normalized)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "SERIAL_DUPLICATE", "Serial numbers must be unique within the request");
                }
            }
        }
    }

    private List<SerialNumber> allocateSerials(Product product,
                                               Warehouse warehouse,
                                               WarehouseBin sourceBin,
                                               StockMovementLineRequest lineRequest,
                                               BigDecimal quantity) {
        return allocateSerials(product, warehouse, sourceBin, lineRequest.batchId(), lineRequest.lotNumber(), lineRequest.serialNumbers(), quantity);
    }

    private List<SerialNumber> allocateSerials(Product product,
                                               Warehouse warehouse,
                                               WarehouseBin sourceBin,
                                               String batchId,
                                               String lotNumber,
                                               List<String> requestedSerials,
                                               BigDecimal quantity) {
        validateSerialQuantity(product, quantity, requestedSerials, false);
        BatchLot batchConstraint = resolveExistingBatchConstraint(product, batchId, lotNumber);
        int unitCount = requireWholeNumber(quantity, "quantity");
        List<SerialNumber> serials;
        if (requestedSerials != null && !requestedSerials.isEmpty()) {
            List<String> normalizedSerials = requestedSerials.stream().map(String::trim).toList();
            serials = serialNumberRepository.findByProductAndSerialsForUpdate(product.getId(), normalizedSerials);
            if (serials.size() != normalizedSerials.size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SERIAL_NOT_FOUND", "One or more serial numbers were not found");
            }
            serials.sort(Comparator.comparing(SerialNumber::getSerialNumber));
        } else {
            serials = stockBoundedSerials(product, warehouse, sourceBin, batchConstraint, unitCount);
        }
        if (serials.size() != unitCount) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_SERIAL_STOCK", "Not enough serial-tracked units are available");
        }
        for (SerialNumber serial : serials) {
            if (!SERIAL_STATUS_IN_STOCK.equals(serial.getSerialStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "SERIAL_UNAVAILABLE", "Serial " + serial.getSerialNumber() + " is not in stock");
            }
            if (serial.getWarehouse() == null || !serial.getWarehouse().getId().equals(warehouse.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SERIAL_WAREHOUSE_MISMATCH", "Serial " + serial.getSerialNumber() + " is not stored in the requested warehouse");
            }
            if (sourceBin != null && (serial.getBin() == null || !serial.getBin().getId().equals(sourceBin.getId()))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SERIAL_BIN_MISMATCH", "Serial " + serial.getSerialNumber() + " is not stored in the requested bin");
            }
            if (batchConstraint != null && (serial.getBatch() == null || !serial.getBatch().getId().equals(batchConstraint.getId()))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SERIAL_BATCH_MISMATCH", "Serial " + serial.getSerialNumber() + " is not stored in the requested batch");
            }
        }
        return serials;
    }

    private List<SerialNumber> stockBoundedSerials(Product product,
                                                   Warehouse warehouse,
                                                   WarehouseBin sourceBin,
                                                   BatchLot batchConstraint,
                                                   int limit) {
        List<SerialNumber> serials = serialNumberRepository.findAvailableForUpdate(
                product.getId(),
                warehouse.getId(),
                sourceBin != null ? sourceBin.getId() : null,
                batchConstraint != null ? batchConstraint.getId() : null
        );
        if (serials.size() <= limit) {
            return serials;
        }
        return serials.subList(0, limit);
    }

    private void createInboundSerials(Product product,
                                      Warehouse warehouse,
                                      WarehouseBin bin,
                                      BatchLot batch,
                                      StockMovementLine movementLine,
                                      List<String> serialNumbers) {
        if (!product.isSerialTracked()) {
            return;
        }
        for (String rawSerial : serialNumbers) {
            String normalized = rawSerial.trim();
            serialNumberRepository.findBySerialNumberAndDeletedAtIsNull(normalized)
                    .ifPresent(existing -> {
                        throw new ApiException(HttpStatus.CONFLICT, "SERIAL_EXISTS", "Serial number already exists: " + normalized);
                    });
            SerialNumber serial = new SerialNumber();
            serial.setProduct(product);
            serial.setBatch(batch);
            serial.setWarehouse(warehouse);
            serial.setBin(bin);
            serial.setSerialNumber(normalized);
            serial.setSerialStatus(SERIAL_STATUS_IN_STOCK);
            serial.setLastMovementLine(movementLine);
            serialNumberRepository.save(serial);
        }
    }

    private StockLevel getOrCreateStockLevel(Product product,
                                             Warehouse warehouse,
                                             WarehouseBin bin,
                                             BatchLot batch) {
        return stockLevelRepository.findForUpdate(
                        product.getId(),
                        warehouse.getId(),
                        bin != null ? bin.getId() : null,
                        batch != null ? batch.getId() : null
                )
                .orElseGet(() -> {
                    StockLevel level = new StockLevel();
                    level.setProduct(product);
                    level.setWarehouse(warehouse);
                    level.setBin(bin);
                    level.setBatch(batch);
                    level.setQuantityOnHand(BigDecimal.ZERO);
                    level.setQuantityReserved(BigDecimal.ZERO);
                    level.setQuantityAvailable(BigDecimal.ZERO);
                    level.setQuantityDamaged(BigDecimal.ZERO);
                    level.setLastMovementAt(Instant.now());
                    return stockLevelRepository.save(level);
                });
    }

    private void increaseStock(StockLevel level, BigDecimal quantity) {
        level.setQuantityOnHand(level.getQuantityOnHand().add(quantity));
        level.setQuantityAvailable(level.getQuantityAvailable().add(quantity));
        level.setLastMovementAt(Instant.now());
        stockLevelRepository.save(level);
    }

    private void decreaseStock(StockLevel level, BigDecimal quantity) {
        if (level.getQuantityAvailable().compareTo(quantity) < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "Insufficient available stock for the requested movement");
        }
        level.setQuantityOnHand(level.getQuantityOnHand().subtract(quantity));
        level.setQuantityAvailable(level.getQuantityAvailable().subtract(quantity));
        level.setLastMovementAt(Instant.now());
        stockLevelRepository.save(level);
    }

    private void ensureFullyAllocated(BigDecimal remaining, Product product) {
        if (remaining.signum() > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INSUFFICIENT_STOCK",
                    "Insufficient stock to complete movement for product " + product.getSku()
            );
        }
    }

    private void reconcileAlerts(Set<ProductWarehouseKey> affectedKeys) {
        for (ProductWarehouseKey key : affectedKeys) {
            StockPolicy stockPolicy = stockPolicyRepository.findByProduct_IdAndWarehouse_Id(key.productId(), key.warehouseId())
                    .or(() -> stockPolicyRepository.findByProduct_IdAndWarehouseIsNull(key.productId()))
                    .orElse(null);
            BigDecimal currentQuantity = stockLevelRepository.sumAvailableByProductAndWarehouse(key.productId(), key.warehouseId());
            ReorderAlert existingAlert = reorderAlertRepository
                    .findFirstByProduct_IdAndWarehouse_IdAndResolvedAtIsNullOrderByCreatedAtDesc(key.productId(), key.warehouseId())
                    .orElse(null);
            if (stockPolicy == null || stockPolicy.getLowStockThreshold() == null || stockPolicy.getLowStockThreshold().signum() <= 0) {
                resolveAlert(existingAlert, currentQuantity);
                continue;
            }
            BigDecimal threshold = stockPolicy.getLowStockThreshold();
            if (currentQuantity.compareTo(threshold) <= 0) {
                ReorderAlert alert = existingAlert != null ? existingAlert : new ReorderAlert();
                if (existingAlert == null) {
                    alert.setProduct(requireProduct(key.productId()));
                    alert.setWarehouse(requireWarehouse(key.warehouseId()));
                }
                alert.setStockPolicy(stockPolicy);
                alert.setCurrentQuantity(currentQuantity);
                alert.setThresholdQuantity(threshold);
                alert.setAlertStatus(ALERT_STATUS_OPEN);
                alert.setResolvedAt(null);
                reorderAlertRepository.save(alert);
                inventoryWebhookPublisher.publish("stock.low", alertPayload(alert, currentQuantity, threshold));
            } else {
                resolveAlert(existingAlert, currentQuantity);
            }
        }
    }

    private void resolveAlert(ReorderAlert alert, BigDecimal currentQuantity) {
        if (alert == null) {
            return;
        }
        alert.setCurrentQuantity(currentQuantity);
        alert.setAlertStatus(ALERT_STATUS_RESOLVED);
        alert.setResolvedAt(Instant.now());
        reorderAlertRepository.save(alert);
        inventoryWebhookPublisher.publish("stock.low.resolved", alertPayload(alert, currentQuantity, alert.getThresholdQuantity()));
    }

    private IamUser resolveCurrentUser() {
        return InventorySecurityContext.currentPrincipal()
                .flatMap(principal -> iamUserRepository.findByIdAndDeletedAtIsNull(principal.userId()))
                .orElse(null);
    }

    private String buildMovementNumber(String movementType) {
        return "INV-" + movementType + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private BigDecimal requirePositiveQuantity(BigDecimal quantity, String fieldName) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QUANTITY_INVALID", fieldName + " must be greater than zero");
        }
        return quantity.stripTrailingZeros();
    }

    private BigDecimal requireNonZeroQuantity(BigDecimal quantity, String fieldName) {
        if (quantity == null || quantity.signum() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QUANTITY_INVALID", fieldName + " must not be zero");
        }
        return quantity.stripTrailingZeros();
    }

    private int requireWholeNumber(BigDecimal quantity, String fieldName) {
        try {
            return quantity.stripTrailingZeros().intValueExact();
        } catch (ArithmeticException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QUANTITY_INTEGER_REQUIRED", fieldName + " must be a whole number for serial-tracked products");
        }
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean sameId(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        return left != null && left.equals(right);
    }

    private StockMovementResponse toMovementResponse(StockMovement movement, List<StockMovementLine> lines) {
        List<StockMovementLineResponse> lineResponses = lines.stream()
                .map(this::toMovementLineResponse)
                .toList();
        return new StockMovementResponse(
                movement.getId(),
                movement.getMovementNumber(),
                movement.getMovementType(),
                movement.getMovementStatus(),
                movement.getSourceWarehouse() != null ? movement.getSourceWarehouse().getId() : null,
                movement.getSourceWarehouse() != null ? movement.getSourceWarehouse().getWarehouseCode() : null,
                movement.getSourceBin() != null ? movement.getSourceBin().getId() : null,
                movement.getSourceBin() != null ? movement.getSourceBin().getBinCode() : null,
                movement.getDestinationWarehouse() != null ? movement.getDestinationWarehouse().getId() : null,
                movement.getDestinationWarehouse() != null ? movement.getDestinationWarehouse().getWarehouseCode() : null,
                movement.getDestinationBin() != null ? movement.getDestinationBin().getId() : null,
                movement.getDestinationBin() != null ? movement.getDestinationBin().getBinCode() : null,
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getExternalReference(),
                movement.getNotes(),
                movement.getProcessedBy() != null ? movement.getProcessedBy().getUsername() : null,
                movement.getProcessedAt(),
                movement.getCreatedAt(),
                lineResponses
        );
    }

    private StockMovementLineResponse toMovementLineResponse(StockMovementLine line) {
        return new StockMovementLineResponse(
                line.getLineNumber(),
                line.getProduct().getId(),
                line.getProduct().getSku(),
                line.getProduct().getName(),
                line.getBatch() != null ? line.getBatch().getId() : null,
                line.getBatch() != null ? line.getBatch().getLotNumber() : null,
                line.getFromBin() != null ? line.getFromBin().getId() : null,
                line.getFromBin() != null ? line.getFromBin().getBinCode() : null,
                line.getToBin() != null ? line.getToBin().getId() : null,
                line.getToBin() != null ? line.getToBin().getBinCode() : null,
                line.getQuantity(),
                line.getUnitCost(),
                line.getExpiryDate(),
                line.getNotes()
        );
    }

    private void publishMovementEvent(StockMovement movement, List<StockMovementLine> lines) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("movementId", movement.getId());
        payload.put("movementNumber", movement.getMovementNumber());
        payload.put("movementType", movement.getMovementType());
        payload.put("movementStatus", movement.getMovementStatus());
        payload.put("referenceType", movement.getReferenceType());
        payload.put("referenceId", movement.getReferenceId());
        payload.put("externalReference", movement.getExternalReference());
        payload.put("processedAt", movement.getProcessedAt());
        payload.put("sourceWarehouseId", movement.getSourceWarehouse() != null ? movement.getSourceWarehouse().getId() : null);
        payload.put("destinationWarehouseId", movement.getDestinationWarehouse() != null ? movement.getDestinationWarehouse().getId() : null);
        payload.put("lines", lines.stream().map(line -> {
            Map<String, Object> linePayload = new LinkedHashMap<>();
            linePayload.put("lineNumber", line.getLineNumber());
            linePayload.put("productId", line.getProduct().getId());
            linePayload.put("quantity", line.getQuantity());
            linePayload.put("batchId", line.getBatch() != null ? line.getBatch().getId() : null);
            linePayload.put("lotNumber", line.getBatch() != null ? line.getBatch().getLotNumber() : null);
            return linePayload;
        }).toList());
        inventoryWebhookPublisher.publish("stock.changed", payload);
    }

    private Map<String, Object> alertPayload(ReorderAlert alert, BigDecimal currentQuantity, BigDecimal threshold) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alertId", alert.getId());
        payload.put("productId", alert.getProduct() != null ? alert.getProduct().getId() : null);
        payload.put("warehouseId", alert.getWarehouse() != null ? alert.getWarehouse().getId() : null);
        payload.put("currentQuantity", currentQuantity);
        payload.put("thresholdQuantity", threshold);
        payload.put("alertStatus", alert.getAlertStatus());
        payload.put("resolvedAt", alert.getResolvedAt());
        return payload;
    }

    private record ProductWarehouseKey(String productId, String warehouseId) {
    }
}
