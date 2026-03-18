package com.noura.shipping.repository;

import com.noura.shipping.domain.entity.MerchantRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Merchant compatibility repository.
 */
public interface MerchantRecordRepository extends JpaRepository<MerchantRecord, UUID>, JpaSpecificationExecutor<MerchantRecord> {

    Optional<MerchantRecord> findByMerchantCodeIgnoreCase(String merchantCode);
}
