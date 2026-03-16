package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.batch.BatchLotFilter;
import com.noura.platform.inventory.dto.batch.BatchLotResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BatchLotQueryService {

    Page<BatchLotResponse> listBatches(BatchLotFilter filter, Pageable pageable);

    BatchLotResponse getBatch(String batchId);
}
