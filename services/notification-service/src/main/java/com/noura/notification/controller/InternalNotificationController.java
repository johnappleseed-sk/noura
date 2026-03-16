package com.noura.notification.controller;

import com.noura.notification.common.ApiResponse;
import com.noura.notification.config.InternalApiProperties;
import com.noura.notification.dto.InternalNotificationCommandRequest;
import com.noura.notification.dto.NotificationDispatchResponse;
import com.noura.notification.service.NotificationMessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private final NotificationMessageService notificationMessageService;
    private final InternalApiProperties internalApiProperties;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationDispatchResponse>> create(
            @Valid @RequestBody InternalNotificationCommandRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedApiKey,
            HttpServletRequest httpRequest
    ) {
        validateInternalApiKey(providedApiKey);
        NotificationDispatchResponse response = notificationMessageService.createAndDispatch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Notification dispatched", response, httpRequest.getRequestURI()));
    }

    private void validateInternalApiKey(String providedApiKey) {
        String configuredApiKey = trimToNull(internalApiProperties.getApiKey());
        if (configuredApiKey == null) {
            return;
        }
        if (!configuredApiKey.equals(trimToNull(providedApiKey))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

