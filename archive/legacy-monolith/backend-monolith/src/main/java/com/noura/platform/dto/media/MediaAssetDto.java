package com.noura.platform.dto.media;

public record MediaAssetDto(
        String url,
        String relativePath,
        String mimeType,
        long sizeBytes,
        String sha256,
        boolean duplicate
) {
}
