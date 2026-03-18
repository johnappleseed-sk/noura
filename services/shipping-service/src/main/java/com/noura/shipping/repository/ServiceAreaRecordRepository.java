package com.noura.shipping.repository;

import com.noura.shipping.domain.entity.ServiceAreaRecord;
import com.noura.shipping.domain.enums.ServiceAreaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

/**
 * Service-area compatibility repository.
 */
public interface ServiceAreaRecordRepository extends JpaRepository<ServiceAreaRecord, UUID>, JpaSpecificationExecutor<ServiceAreaRecord> {

    List<ServiceAreaRecord> findByDeletedAtIsNullAndStatus(ServiceAreaStatus status);
}
