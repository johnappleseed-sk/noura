package com.noura.platform.service;

import org.springframework.web.multipart.MultipartFile;

public interface MediaAssetService {

    StoredMediaAsset upload(MultipartFile file);

    StoredMediaAsset importFromUrl(String sourceUrl);

    record StoredMediaAsset(
            String relativePath,
            String mimeType,
            long sizeBytes,
            String sha256,
            boolean duplicate
    ) {
    }
}
