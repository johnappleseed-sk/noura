package com.noura.platform.repository;

import com.noura.platform.domain.entity.MerchantContractAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantContractActionRepository extends JpaRepository<MerchantContractAction, UUID> {
    List<MerchantContractAction> findByContractIdOrderByOccurredAtDesc(UUID contractId);
}

