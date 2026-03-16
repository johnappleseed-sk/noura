package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.config.MediaStorageProperties;
import com.noura.platform.service.MediaAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MediaAssetServiceImpl implements MediaAssetService {

    private static final String PRODUCT_MEDIA_FOLDER = "product-media";

    private final MediaStorageProperties mediaStorageProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public StoredMediaAsset upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("MEDIA_UPLOAD_EMPTY", "Image file is required.");
        }

        byte[] bytes = readAndValidateSize(file);
        DetectedImage detected = detectImage(bytes, file.getContentType());
        return persist(bytes, detected);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public StoredMediaAsset importFromUrl(String sourceUrl) {
        URI uri = safeHttpUri(sourceUrl);
        validateRemoteHost(uri.getHost());

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(Math.max(1_000, mediaStorageProperties.getImportTimeoutMs())))
                .header("Accept", "image/*")
                .GET()
                .build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BadRequestException("MEDIA_URL_FETCH_FAILED", "Unable to fetch image from the provided URL.");
        }

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new BadRequestException("MEDIA_URL_FETCH_FAILED", "Image URL responded with status " + status + ".");
        }

        long maxBytes = Math.max(1, mediaStorageProperties.getMaxBytes());
        response.headers().firstValueAsLong("Content-Length").ifPresent(contentLength -> {
            if (contentLength > maxBytes) {
                throw new BadRequestException("MEDIA_FILE_TOO_LARGE", "Image exceeds max size of " + maxBytes + " bytes.");
            }
        });

        byte[] bytes;
        try (InputStream body = response.body()) {
            bytes = readLimited(body, maxBytes);
        } catch (IOException ex) {
            throw new BadRequestException("MEDIA_URL_FETCH_FAILED", "Unable to read image bytes from URL.");
        }
        if (bytes.length == 0) {
            throw new BadRequestException("MEDIA_UPLOAD_EMPTY", "Downloaded image is empty.");
        }

        String contentType = response.headers().firstValue("Content-Type").orElse(null);
        DetectedImage detected = detectImage(bytes, contentType);
        return persist(bytes, detected);
    }

    private byte[] readAndValidateSize(MultipartFile file) {
        long maxBytes = Math.max(1, mediaStorageProperties.getMaxBytes());
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("MEDIA_FILE_TOO_LARGE", "Image exceeds max size of " + maxBytes + " bytes.");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return readLimited(inputStream, maxBytes);
        } catch (IOException ex) {
            throw new BadRequestException("MEDIA_UPLOAD_INVALID", "Unable to read uploaded image.");
        }
    }

    private byte[] readLimited(InputStream stream, long maxBytes) throws IOException {
        byte[] buffer = new byte[8_192];
        int read;
        long total = 0;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((read = stream.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new BadRequestException("MEDIA_FILE_TOO_LARGE", "Image exceeds max size of " + maxBytes + " bytes.");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private DetectedImage detectImage(byte[] bytes, String contentType) {
        if (isJpeg(bytes)) {
            return new DetectedImage("jpg", "image/jpeg");
        }
        if (isPng(bytes)) {
            return new DetectedImage("png", "image/png");
        }
        if (isWebp(bytes)) {
            return new DetectedImage("webp", "image/webp");
        }
        if (isGif(bytes)) {
            return new DetectedImage("gif", "image/gif");
        }

        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (mediaStorageProperties.isAllowSvg() || normalizedContentType.contains("svg")) {
            if (!mediaStorageProperties.isAllowSvg()) {
                throw new BadRequestException("MEDIA_SVG_DISABLED", "SVG uploads are disabled.");
            }
            if (looksLikeSvg(bytes)) {
                validateSvg(bytes);
                return new DetectedImage("svg", "image/svg+xml");
            }
        }

        throw new BadRequestException(
                "MEDIA_TYPE_UNSUPPORTED",
                "Only jpg, jpeg, png, webp, gif" + (mediaStorageProperties.isAllowSvg() ? ", svg" : "") + " are supported."
        );
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isGif(byte[] bytes) {
        return bytes.length >= 6
                && bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == '8'
                && (bytes[4] == '7' || bytes[4] == '9')
                && bytes[5] == 'a';
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }

    private boolean looksLikeSvg(byte[] bytes) {
        String head = new String(bytes, 0, Math.min(bytes.length, 2048), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        return head.contains("<svg");
    }

    private void validateSvg(byte[] bytes) {
        String svg = new String(bytes, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        if (!svg.contains("<svg")) {
            throw new BadRequestException("MEDIA_SVG_INVALID", "SVG payload is invalid.");
        }
        if (svg.contains("<script")
                || svg.contains("javascript:")
                || svg.contains("<iframe")
                || svg.contains("<object")
                || svg.contains("<embed")
                || svg.contains("<foreignobject")
                || svg.contains("onload=")
                || svg.contains("onerror=")
                || svg.contains("onclick=")
                || svg.contains("onmouseover=")) {
            throw new BadRequestException("MEDIA_SVG_UNSAFE", "SVG contains unsafe content.");
        }
    }

    private StoredMediaAsset persist(byte[] bytes, DetectedImage detected) {
        String sha = sha256Hex(bytes);
        Path uploadRoot = Path.of(mediaStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        Path mediaRoot = uploadRoot.resolve(PRODUCT_MEDIA_FOLDER).normalize();
        try {
            Files.createDirectories(mediaRoot);
        } catch (IOException ex) {
            throw new BadRequestException("MEDIA_STORAGE_UNAVAILABLE", "Unable to prepare media storage.");
        }

        String fileName = sha + "." + detected.extension();
        Path target = mediaRoot.resolve(fileName).normalize();
        if (!target.startsWith(mediaRoot)) {
            throw new BadRequestException("MEDIA_STORAGE_INVALID", "Resolved media path is invalid.");
        }

        boolean duplicate = Files.exists(target);
        if (!duplicate) {
            Path temp = mediaRoot.resolve(fileName + ".tmp").normalize();
            try {
                Files.write(temp, bytes);
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ex) {
                throw new BadRequestException("MEDIA_STORAGE_WRITE_FAILED", "Failed to store uploaded image.");
            } finally {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            }
        }

        String relativePath = normalizePublicPrefix(mediaStorageProperties.getPublicPathPrefix())
                + "/" + PRODUCT_MEDIA_FOLDER
                + "/" + fileName;
        return new StoredMediaAsset(relativePath, detected.mimeType(), bytes.length, sha, duplicate);
    }

    private URI safeHttpUri(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.trim().isEmpty()) {
            throw new BadRequestException("MEDIA_URL_REQUIRED", "Image URL is required.");
        }
        URI uri;
        try {
            uri = URI.create(sourceUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("MEDIA_URL_INVALID", "Image URL is invalid.");
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new BadRequestException("MEDIA_URL_INVALID", "Only http/https image URLs are supported.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new BadRequestException("MEDIA_URL_INVALID", "Image URL host is required.");
        }
        return uri;
    }

    private void validateRemoteHost(String host) {
        String normalized = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || "localhost".equals(normalized) || normalized.endsWith(".local")) {
            throw new BadRequestException("MEDIA_URL_HOST_BLOCKED", "Local/private hosts are not allowed.");
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(normalized);
            for (InetAddress address : addresses) {
                if (isPrivateAddress(address)) {
                    throw new BadRequestException("MEDIA_URL_HOST_BLOCKED", "Local/private hosts are not allowed.");
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("MEDIA_URL_INVALID", "Unable to resolve URL host.");
        }
    }

    private boolean isPrivateAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        if (address instanceof Inet4Address ipv4) {
            byte[] octets = ipv4.getAddress();
            int first = octets[0] & 0xFF;
            int second = octets[1] & 0xFF;
            return first == 10
                    || first == 127
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168)
                    || (first == 169 && second == 254);
        }
        if (address instanceof Inet6Address ipv6) {
            byte[] bytes = ipv6.getAddress();
            int first = bytes[0] & 0xFF;
            return first == 0xFC || first == 0xFD;
        }
        return false;
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private String normalizePublicPrefix(String rawPrefix) {
        String normalized = rawPrefix == null ? "/uploads" : rawPrefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private record DetectedImage(String extension, String mimeType) {
    }
}
