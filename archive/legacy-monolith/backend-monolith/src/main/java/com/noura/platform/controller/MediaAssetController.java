package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.media.MediaAssetDto;
import com.noura.platform.dto.media.MediaImportUrlRequest;
import com.noura.platform.service.MediaAssetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/media-assets")
public class MediaAssetController {

    private final MediaAssetService mediaAssetService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaAssetDto>> upload(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest http
    ) {
        MediaAssetService.StoredMediaAsset stored = mediaAssetService.upload(file);
        MediaAssetDto dto = toDto(stored);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Media uploaded", dto, http.getRequestURI()));
    }

    @PostMapping("/import-url")
    public ResponseEntity<ApiResponse<MediaAssetDto>> importFromUrl(
            @Valid @RequestBody MediaImportUrlRequest request,
            HttpServletRequest http
    ) {
        MediaAssetService.StoredMediaAsset stored = mediaAssetService.importFromUrl(request.url());
        MediaAssetDto dto = toDto(stored);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Media imported", dto, http.getRequestURI()));
    }

    private MediaAssetDto toDto(MediaAssetService.StoredMediaAsset stored) {
        String publicUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(stored.relativePath())
                .toUriString();
        return new MediaAssetDto(
                publicUrl,
                stored.relativePath(),
                stored.mimeType(),
                stored.sizeBytes(),
                stored.sha256(),
                stored.duplicate()
        );
    }
}
