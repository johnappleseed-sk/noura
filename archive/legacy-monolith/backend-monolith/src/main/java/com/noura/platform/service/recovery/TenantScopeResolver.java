package com.noura.platform.service.recovery;

import com.noura.platform.config.RecoveryProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves the effective tenant scope for recovery-governed operations.
 */
@Service
public class TenantScopeResolver {
    private static final String TENANT_HEADER = "X-Tenant-Key";

    private final RecoveryProperties recoveryProperties;

    /**
     * Creates a new tenant scope resolver.
     *
     * @param recoveryProperties The recovery configuration properties.
     */
    public TenantScopeResolver(RecoveryProperties recoveryProperties) {
        this.recoveryProperties = recoveryProperties;
    }

    /**
     * Resolves the current request tenant or falls back to the configured default tenant.
     *
     * @return The effective tenant key.
     */
    public String resolveCurrentTenant() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String tenantHeader = attributes.getRequest().getHeader(TENANT_HEADER);
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                return tenantHeader.trim();
            }
        }
        return recoveryProperties.getDefaultTenantKey();
    }
}
