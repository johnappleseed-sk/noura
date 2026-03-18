package com.noura.shipping.repository;

import com.noura.shipping.domain.entity.StoreRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store compatibility repository.
 */
public interface StoreRecordRepository extends JpaRepository<StoreRecord, UUID>, JpaSpecificationExecutor<StoreRecord> {

    Optional<StoreRecord> findByStoreCodeIgnoreCase(String storeCode);

    List<StoreRecord> findByDeletedAtIsNullAndStatus(com.noura.shipping.domain.enums.StoreStatus status);
}
