package com.noura.platform.repository;

import com.noura.platform.domain.entity.MerchandisingBoost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MerchandisingBoostRepository extends JpaRepository<MerchandisingBoost, UUID> {
}
