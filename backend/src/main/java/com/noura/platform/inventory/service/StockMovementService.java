package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.stock.AdjustmentMovementRequest;
import com.noura.platform.inventory.dto.stock.InboundMovementRequest;
import com.noura.platform.inventory.dto.stock.OutboundMovementRequest;
import com.noura.platform.inventory.dto.stock.ReturnMovementRequest;
import com.noura.platform.inventory.dto.stock.StockMovementFilter;
import com.noura.platform.inventory.dto.stock.StockMovementResponse;
import com.noura.platform.inventory.dto.stock.TransferMovementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockMovementService {

    StockMovementResponse receiveInbound(InboundMovementRequest request);

    StockMovementResponse shipOutbound(OutboundMovementRequest request);

    StockMovementResponse returnStock(ReturnMovementRequest request);

    StockMovementResponse transferStock(TransferMovementRequest request);

    StockMovementResponse adjustStock(AdjustmentMovementRequest request);

    Page<StockMovementResponse> listMovements(StockMovementFilter filter, Pageable pageable);

    StockMovementResponse getMovement(String movementId);
}
