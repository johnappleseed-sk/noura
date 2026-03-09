package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.stock.StockLevelFilter;
import com.noura.platform.inventory.dto.stock.StockLevelResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockLevelService {

    Page<StockLevelResponse> listStockLevels(StockLevelFilter filter, Pageable pageable);
}
