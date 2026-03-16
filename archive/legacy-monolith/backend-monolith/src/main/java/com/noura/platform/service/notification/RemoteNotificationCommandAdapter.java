package com.noura.platform.service.notification;

import com.noura.platform.common.exception.ServiceUnavailableException;
import com.noura.platform.config.AppProperties;
import com.noura.platform.config.CorrelationIdFilter;
import com.noura.platform.domain.enums.NotificationCategory;
import com.noura.platform.dto.notification.SendNotificationRequest;
import com.noura.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * Remote adapter used when notification extraction is enabled.
 * Falls back to local dispatch by configuration to keep runtime behavior stable during migration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.remote", name = "enabled", havingValue = "true")
public class RemoteNotificationCommandAdapter implements NotificationCommandPort {

    private final AppProperties appProperties;
    private final NotificationService localNotificationService;
    private final RestTemplateBuilder restTemplateBuilder;

    @Override
    public void pushToUser(UUID targetUserId, SendNotificationRequest request) {
        AppProperties.Notifications.Remote remote = appProperties.getNotifications().getRemote();
        String baseUrl = normalizeBaseUrl(remote.getBaseUrl());
        String commandPath = normalizeCommandPath(remote.getCommandPath());
        if (baseUrl == null) {
            fallbackOrThrow(targetUserId, request, null);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String internalApiKey = trimToNull(remote.getInternalApiKey());
        if (internalApiKey != null) {
            headers.set("X-Internal-Api-Key", internalApiKey);
        }
        String correlationId = trimToNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE));
        if (correlationId != null) {
            headers.set(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
        }

        RestTemplate restTemplate = buildRestTemplate(remote);

        InternalNotificationCommandRequest payload = new InternalNotificationCommandRequest(
                targetUserId,
                request.category(),
                request.title(),
                request.body()
        );

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    baseUrl + commandPath,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Void.class
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceUnavailableException(
                        "NOTIFICATION_REMOTE_ERROR",
                        "Remote notification service returned non-success status"
                );
            }
        } catch (RestClientException | ServiceUnavailableException exception) {
            fallbackOrThrow(targetUserId, request, exception);
        }
    }

    private void fallbackOrThrow(UUID targetUserId, SendNotificationRequest request, Exception exception) {
        if (appProperties.getNotifications().getRemote().isFallbackToLocal()) {
            if (exception == null) {
                log.warn("Notification remote base URL is not configured. Falling back to local notification dispatch.");
            } else {
                log.warn("Remote notification dispatch failed. Falling back to local notification dispatch.", exception);
            }
            localNotificationService.pushToUser(targetUserId, request);
            return;
        }
        throw new ServiceUnavailableException(
                "NOTIFICATION_REMOTE_UNAVAILABLE",
                "Notification service is unavailable and local fallback is disabled"
        );
    }

    private String normalizeBaseUrl(String value) {
        String baseUrl = trimToNull(value);
        if (baseUrl == null) {
            return null;
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl.isBlank() ? null : baseUrl;
    }

    private String normalizeCommandPath(String value) {
        String path = trimToNull(value);
        if (path == null) {
            return "/internal/notifications";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private RestTemplate buildRestTemplate(AppProperties.Notifications.Remote remote) {
        int connectTimeoutMs = Math.max(500, remote.getConnectTimeoutMs());
        int readTimeoutMs = Math.max(500, remote.getReadTimeoutMs());
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record InternalNotificationCommandRequest(
            UUID targetUserId,
            NotificationCategory category,
            String title,
            String body
    ) {
    }
}
