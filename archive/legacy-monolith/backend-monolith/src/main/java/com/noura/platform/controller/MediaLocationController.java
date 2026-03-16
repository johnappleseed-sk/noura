package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.location.PhotoLocationMetadataDto;
import com.noura.platform.dto.location.PhotoLocationMetadataRequest;
import com.noura.platform.service.PhotoLocationMetadataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MediaLocationController {

    private final PhotoLocationMetadataService photoLocationMetadataService;

    @PostMapping("${app.api.version-prefix:/api/v1}/media/{mediaId}/extract-location")
    public ResponseEntity<ApiResponse<PhotoLocationMetadataDto>> extract(
            @PathVariable UUID mediaId,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        PhotoLocationMetadataDto dto = photoLocationMetadataService.extract(mediaId, actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Photo location extracted", dto, http.getRequestURI()));
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/media/{mediaId}/location")
    public ApiResponse<PhotoLocationMetadataDto> get(@PathVariable UUID mediaId, HttpServletRequest http) {
        return ApiResponse.ok("Photo location", photoLocationMetadataService.get(mediaId), http.getRequestURI());
    }

    @PutMapping("${app.api.version-prefix:/api/v1}/media/{mediaId}/location")
    public ApiResponse<PhotoLocationMetadataDto> upsert(
            @PathVariable UUID mediaId,
            @Valid @RequestBody PhotoLocationMetadataRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Photo location updated", photoLocationMetadataService.upsert(mediaId, request, actor), http.getRequestURI());
    }

    @DeleteMapping("${app.api.version-prefix:/api/v1}/media/{mediaId}/location")
    public ApiResponse<Void> delete(
            @PathVariable UUID mediaId,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        photoLocationMetadataService.delete(mediaId, actor);
        return ApiResponse.ok("Photo location removed", null, http.getRequestURI());
    }
}

