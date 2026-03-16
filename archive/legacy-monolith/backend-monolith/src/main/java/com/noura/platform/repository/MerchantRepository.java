package com.noura.platform.repository;

import com.noura.platform.domain.entity.Merchant;
import com.noura.platform.domain.enums.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID>, JpaSpecificationExecutor<Merchant> {
    Optional<Merchant> findByNameIgnoreCase(String name);

    Optional<Merchant> findByMerchantCodeIgnoreCase(String merchantCode);

    boolean existsByMerchantCodeIgnoreCase(String merchantCode);

    long countByStatus(MerchantStatus status);
}
