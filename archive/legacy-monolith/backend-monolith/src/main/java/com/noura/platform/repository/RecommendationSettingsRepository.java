package com.noura.platform.repository;

import com.noura.platform.domain.entity.RecommendationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecommendationSettingsRepository extends JpaRepository<RecommendationSettings, UUID> {
}
