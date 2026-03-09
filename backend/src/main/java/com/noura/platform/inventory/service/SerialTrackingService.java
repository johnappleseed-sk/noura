package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.serial.SerialNumberFilter;
import com.noura.platform.inventory.dto.serial.SerialNumberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SerialTrackingService {

    Page<SerialNumberResponse> listSerials(SerialNumberFilter filter, Pageable pageable);

    SerialNumberResponse getSerial(String serialId);
}
