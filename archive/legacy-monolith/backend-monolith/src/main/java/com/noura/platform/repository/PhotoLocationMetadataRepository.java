package com.noura.platform.repository;

import com.noura.platform.domain.entity.PhotoLocationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PhotoLocationMetadataRepository extends JpaRepository<PhotoLocationMetadata, UUID> {
    Optional<PhotoLocationMetadata> findByMediaId(UUID mediaId);
}

