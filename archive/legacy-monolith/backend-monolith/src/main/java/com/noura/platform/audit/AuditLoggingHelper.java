package com.noura.platform.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLoggingHelper {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final UserAccountRepository userAccountRepository;

    public void logAction(String actionCode, String entityType, UUID entityId, Object oldValue, Object newValue) {
        try {
            ActorContext actor = resolveActor();
            RequestContext requestContext = resolveRequestContext();
            auditLogService.record(new AuditLogService.AuditLogCommand(
                    actor.actorUserId(),
                    actor.actorUsername(),
                    actionCode,
                    entityType,
                    entityId,
                    toJson(oldValue),
                    toJson(newValue),
                    requestContext.requestPath(),
                    requestContext.requestMethod(),
                    requestContext.ipAddress()
            ));
        } catch (Exception ex) {
            log.warn("Audit logging failed for action={} entityType={} entityId={}", actionCode, entityType, entityId, ex);
        }
    }

    private ActorContext resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return new ActorContext(null, "system");
        }

        String principalName = authentication.getName();
        if (principalName == null || principalName.isBlank()) {
            return new ActorContext(null, "system");
        }

        return userAccountRepository.findByEmailIgnoreCase(principalName)
                .map(this::toActorContext)
                .orElseGet(() -> new ActorContext(null, principalName));
    }

    private ActorContext toActorContext(UserAccount user) {
        String actorUsername = user.getFullName();
        if (actorUsername == null || actorUsername.isBlank()) {
            actorUsername = user.getEmail();
        }
        return new ActorContext(user.getId(), actorUsername);
    }

    private RequestContext resolveRequestContext() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return new RequestContext(null, null, null);
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        if (request == null) {
            return new RequestContext(null, null, null);
        }

        String forwardedFor = firstNonBlank(
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
                request.getRemoteAddr()
        );
        String ipAddress = forwardedFor;
        if (forwardedFor != null && forwardedFor.contains(",")) {
            ipAddress = forwardedFor.substring(0, forwardedFor.indexOf(',')).trim();
        }

        return new RequestContext(
                request.getRequestURI(),
                request.getMethod(),
                ipAddress
        );
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            try {
                return objectMapper.writeValueAsString(Map.of("value", String.valueOf(value)));
            } catch (JsonProcessingException ignored) {
                return "{\"value\":\"serialization_failed\"}";
            }
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record ActorContext(UUID actorUserId, String actorUsername) {
    }

    private record RequestContext(String requestPath, String requestMethod, String ipAddress) {
    }
}
