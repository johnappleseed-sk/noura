package com.noura.platform.repository;

import com.noura.platform.domain.entity.StoreTenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreTenantRepository extends JpaRepository<StoreTenant, UUID> {
    Optional<StoreTenant> findByStoreId(UUID storeId);

    List<StoreTenant> findByMerchantId(UUID merchantId);

    List<StoreTenant> findByContractId(UUID contractId);
}

