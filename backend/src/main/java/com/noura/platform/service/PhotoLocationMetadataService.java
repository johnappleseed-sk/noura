package com.noura.platform.service;

import com.noura.platform.dto.location.PhotoLocationMetadataDto;
import com.noura.platform.dto.location.PhotoLocationMetadataRequest;

import java.util.UUID;

public interface PhotoLocationMetadataService {
    PhotoLocationMetadataDto extract(UUID mediaId, String actor);

    PhotoLocationMetadataDto get(UUID mediaId);

    PhotoLocationMetadataDto upsert(UUID mediaId, PhotoLocationMetadataRequest request, String actor);

    void delete(UUID mediaId, String actor);
}

