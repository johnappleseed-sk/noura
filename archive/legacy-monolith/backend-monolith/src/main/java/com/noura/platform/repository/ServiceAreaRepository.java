package com.noura.platform.repository;

import com.noura.platform.domain.entity.ServiceArea;
import com.noura.platform.domain.enums.ServiceAreaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ServiceAreaRepository extends JpaRepository<ServiceArea, UUID>, JpaSpecificationExecutor<ServiceArea> {
    List<ServiceArea> findByStatus(ServiceAreaStatus status);
}

