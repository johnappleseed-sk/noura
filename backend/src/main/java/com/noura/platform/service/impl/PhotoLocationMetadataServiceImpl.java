package com.noura.platform.service.impl;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.PhotoLocationMetadata;
import com.noura.platform.domain.entity.ProductMedia;
import com.noura.platform.domain.enums.LocationSource;
import com.noura.platform.domain.enums.PhotoPrivacyLevel;
import com.noura.platform.dto.location.PhotoLocationMetadataDto;
import com.noura.platform.dto.location.PhotoLocationMetadataRequest;
import com.noura.platform.dto.location.ReverseGeocodeRequest;
import com.noura.platform.repository.PhotoLocationMetadataRepository;
import com.noura.platform.repository.ProductMediaRepository;
import com.noura.platform.service.LocationGeocodingService;
import com.noura.platform.service.OptionalCommerceAuditService;
import com.noura.platform.service.PhotoLocationMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoLocationMetadataServiceImpl implements PhotoLocationMetadataService {

    private final PhotoLocationMetadataRepository photoLocationMetadataRepository;
    private final ProductMediaRepository productMediaRepository;
    private final LocationGeocodingService geocodingService;
    private final OptionalCommerceAuditService auditEventService;

    @Value("${app.location.photo-exif.enabled:false}")
    private boolean exifEnabled;

    @Value("${app.location.photo-exif.allowed-hosts:}")
    private String allowedHostsRaw;

    @Value("${app.location.photo-exif.max-bytes:6000000}")
    private int maxBytes;

    @Value("${app.location.photo-exif.timeout-ms:4500}")
    private int timeoutMs;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public PhotoLocationMetadataDto extract(UUID mediaId, String actor) {
        if (!exifEnabled) {
            throw new BadRequestException("PHOTO_EXIF_DISABLED", "Photo EXIF extraction is disabled.");
        }
        Set<String> allowedHosts = parseAllowedHosts(allowedHostsRaw);
        if (allowedHosts.isEmpty()) {
            throw new BadRequestException("PHOTO_EXIF_DISABLED", "No allowed hosts configured for EXIF extraction.");
        }

        ProductMedia media = productMediaRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("MEDIA_NOT_FOUND", "Media not found"));

        URI uri = safeUri(media.getUrl());
        if (!allowedHosts.contains(uri.getHost().toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("PHOTO_EXIF_HOST_BLOCKED", "Media host is not allowed for EXIF extraction.");
        }

        byte[] bytes = downloadBytes(uri);
        ExifResult exif = extractExif(bytes);
        if (exif.location() == null
                || Double.isNaN(exif.location().getLatitude())
                || Double.isNaN(exif.location().getLongitude())
                || exif.location().isZero()) {
            throw new BadRequestException("PHOTO_EXIF_NOT_FOUND", "No EXIF GPS metadata found in this image.");
        }

        BigDecimal latitude = BigDecimal.valueOf(exif.location().getLatitude()).setScale(7, RoundingMode.HALF_UP);
        BigDecimal longitude = BigDecimal.valueOf(exif.location().getLongitude()).setScale(7, RoundingMode.HALF_UP);

        PhotoLocationMetadata existing = photoLocationMetadataRepository.findByMediaId(mediaId).orElse(null);
        PhotoLocationMetadataDto before = existing == null ? null : toDto(existing);
        PhotoLocationMetadata entity = existing == null ? new PhotoLocationMetadata() : existing;
        entity.setMediaId(mediaId);
        entity.setSource(LocationSource.PHOTO_EXIF);
        entity.setCapturedAt(exif.capturedAt() == null ? Instant.now() : exif.capturedAt());
        entity.setLatitude(latitude);
        entity.setLongitude(longitude);
        entity.setAccuracyMeters(null);
        entity.setPrivacyLevel(PhotoPrivacyLevel.INTERNAL);
        entity.setVisibleToAdmin(true);

        // Best-effort reverse geocode snapshot.
        try {
            var geocode = geocodingService.reverseGeocode(new ReverseGeocodeRequest(latitude, longitude, null));
            entity.setAddressSnapshot(geocode == null ? null : geocode.formattedAddress());
        } catch (Exception ignored) {
            // leave addressSnapshot null
        }

        PhotoLocationMetadata saved = photoLocationMetadataRepository.save(entity);
        auditEventService.record(
                "PHOTO_LOCATION_EXTRACTED",
                "PhotoLocationMetadata",
                saved.getId(),
                before,
                toDto(saved),
                Collections.singletonMap("actor", actor)
        );
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PhotoLocationMetadataDto get(UUID mediaId) {
        PhotoLocationMetadata metadata = photoLocationMetadataRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new NotFoundException("PHOTO_LOCATION_NOT_FOUND", "Photo location metadata not found"));
        return toDto(metadata);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public PhotoLocationMetadataDto upsert(UUID mediaId, PhotoLocationMetadataRequest request, String actor) {
        if (request == null) {
            throw new BadRequestException("PHOTO_LOCATION_INVALID", "Request payload is required.");
        }

        PhotoPrivacyLevel privacy = request.privacyLevel() == null ? PhotoPrivacyLevel.INTERNAL : request.privacyLevel();
        if (privacy == PhotoPrivacyLevel.NONE) {
            delete(mediaId, actor);
            throw new BadRequestException("PHOTO_LOCATION_REMOVED", "Photo location metadata removed.");
        }

        boolean hasLat = request.latitude() != null;
        boolean hasLng = request.longitude() != null;
        if (hasLat != hasLng) {
            throw new BadRequestException("PHOTO_LOCATION_COORDINATES_INVALID", "Latitude and longitude must be provided together.");
        }

        PhotoLocationMetadata existing = photoLocationMetadataRepository.findByMediaId(mediaId).orElse(null);
        PhotoLocationMetadataDto before = existing == null ? null : toDto(existing);
        PhotoLocationMetadata entity = existing == null ? new PhotoLocationMetadata() : existing;

        entity.setMediaId(mediaId);
        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setCapturedAt(request.capturedAt());
        entity.setSource(request.source() == null ? LocationSource.ADMIN_INPUT : request.source());
        entity.setAccuracyMeters(request.accuracyMeters());
        entity.setPrivacyLevel(privacy);
        entity.setVisibleToAdmin(request.visibleToAdmin());

        if (request.reverseGeocode() && request.latitude() != null && request.longitude() != null) {
            try {
                var geocode = geocodingService.reverseGeocode(new ReverseGeocodeRequest(request.latitude(), request.longitude(), null));
                entity.setAddressSnapshot(geocode == null ? null : geocode.formattedAddress());
            } catch (Exception ignored) {
                // leave addressSnapshot as-is
            }
        }

        PhotoLocationMetadata saved = photoLocationMetadataRepository.save(entity);
        auditEventService.record(
                "PHOTO_LOCATION_UPSERTED",
                "PhotoLocationMetadata",
                saved.getId(),
                before,
                toDto(saved),
                Collections.singletonMap("actor", actor)
        );
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID mediaId, String actor) {
        PhotoLocationMetadata existing = photoLocationMetadataRepository.findByMediaId(mediaId).orElse(null);
        if (existing == null) return;
        PhotoLocationMetadataDto before = toDto(existing);
        photoLocationMetadataRepository.delete(existing);
        auditEventService.record(
                "PHOTO_LOCATION_DELETED",
                "PhotoLocationMetadata",
                existing.getId(),
                before,
                null,
                Collections.singletonMap("actor", actor)
        );
    }

    private PhotoLocationMetadataDto toDto(PhotoLocationMetadata entity) {
        return new PhotoLocationMetadataDto(
                entity.getId(),
                entity.getMediaId(),
                entity.getOwnerId(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getCapturedAt(),
                entity.getSource(),
                entity.getAccuracyMeters(),
                entity.getAddressSnapshot(),
                entity.getPrivacyLevel(),
                entity.isVisibleToAdmin(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy()
        );
    }

    private record ExifResult(GeoLocation location, Instant capturedAt) {}

    private ExifResult extractExif(byte[] bytes) {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            Metadata metadata = ImageMetadataReader.readMetadata(in);
            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            GeoLocation location = gps == null ? null : gps.getGeoLocation();

            ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            Date original = exif == null ? null : exif.getDateOriginal();
            Instant capturedAt = original == null ? null : original.toInstant();

            return new ExifResult(location, capturedAt);
        } catch (Exception ex) {
            throw new BadRequestException("PHOTO_EXIF_INVALID", "Unable to extract EXIF metadata from image.");
        }
    }

    private boolean hasUsableLocation(GeoLocation location) {
        if (location == null) {
            return false;
        }
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        return !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && latitude >= -90.0d
                && latitude <= 90.0d
                && longitude >= -180.0d
                && longitude <= 180.0d;
    }

    private URI safeUri(String url) {
        try {
            URI uri = URI.create(Optional.ofNullable(url).orElse("").trim());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new BadRequestException("PHOTO_EXIF_URL_INVALID", "Only http/https media URLs are supported.");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new BadRequestException("PHOTO_EXIF_URL_INVALID", "Media URL host is required.");
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("PHOTO_EXIF_URL_INVALID", "Media URL is invalid.");
        }
    }

    private Set<String> parseAllowedHosts(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private byte[] downloadBytes(URI uri) {
        try {
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(Math.max(500, timeoutMs));
            connection.setReadTimeout(Math.max(500, timeoutMs));
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "noura-platform/1.0");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new BadRequestException("PHOTO_EXIF_DOWNLOAD_FAILED", "Failed to download media for EXIF extraction.");
            }

            try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                int total = 0;
                while ((read = in.read(buffer)) >= 0) {
                    total += read;
                    if (total > maxBytes) {
                        throw new BadRequestException("PHOTO_EXIF_TOO_LARGE", "Media is too large to inspect for EXIF metadata.");
                    }
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("PHOTO_EXIF_DOWNLOAD_FAILED", "Failed to download media for EXIF extraction.");
        }
    }
}
