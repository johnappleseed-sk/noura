package com.noura.platform.repository;

import com.noura.platform.domain.entity.MerchantContract;
import com.noura.platform.domain.enums.MerchantContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantContractRepository extends JpaRepository<MerchantContract, UUID>, JpaSpecificationExecutor<MerchantContract> {
    Optional<MerchantContract> findByContractNumberIgnoreCase(String contractNumber);

    List<MerchantContract> findByMerchantIdAndStatusOrderByCreatedAtDesc(UUID merchantId, MerchantContractStatus status);
}

