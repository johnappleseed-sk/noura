package com.noura.platform.repository;

import com.noura.platform.domain.entity.MerchandisingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MerchandisingSettingsRepository extends JpaRepository<MerchandisingSettings, UUID> {
}
