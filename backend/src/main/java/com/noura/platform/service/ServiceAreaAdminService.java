package com.noura.platform.service;

import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;
import com.noura.platform.dto.location.ServiceAreaDto;
import com.noura.platform.dto.location.ServiceAreaRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ServiceAreaAdminService {
    Page<ServiceAreaDto> list(String query, ServiceAreaStatus status, ServiceAreaType type, Pageable pageable);

    ServiceAreaDto get(UUID serviceAreaId);

    ServiceAreaDto create(ServiceAreaRequest request, String actor);

    ServiceAreaDto update(UUID serviceAreaId, ServiceAreaRequest request, String actor);

    void delete(UUID serviceAreaId, String actor);

    ServiceAreaDto activate(UUID serviceAreaId, String actor);

    ServiceAreaDto deactivate(UUID serviceAreaId, String actor);
}

