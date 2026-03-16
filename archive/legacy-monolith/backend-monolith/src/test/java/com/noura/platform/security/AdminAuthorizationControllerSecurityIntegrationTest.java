package com.noura.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.config.CorrelationIdFilter;
import com.noura.platform.config.RateLimitFilter;
import com.noura.platform.config.SecurityConfig;
import com.noura.platform.controller.AdminAuthorizationController;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentPreviewDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentPreviewItemDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentResultDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleViewDto;
import com.noura.platform.dto.admin.AdminPermissionDto;
import com.noura.platform.dto.admin.AdminPermissionPresetDto;
import com.noura.platform.dto.admin.AdminRbacAuditLogDto;
import com.noura.platform.dto.admin.AdminRolePermissionDto;
import com.noura.platform.dto.admin.AdminUserRoleAssignmentDto;
import com.noura.platform.service.AdminRoleManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Module: Admin Authorization Security
 * Purpose: Verifies method-level role restrictions for RBAC management endpoints.
 * Responsibilities:
 * - Ensure anonymous requests are blocked.
 * - Ensure role and permission authorities are enforced for each endpoint.
 * - Ensure expected payload contracts are returned for authorized callers.
 * Related modules:
 * - AdminAuthorizationController
 * - AdminRoleManagementService
 * - SecurityConfig
 */
@WebMvcTest(controllers = AdminAuthorizationController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        CorrelationIdFilter.class,
        RateLimitFilter.class
})
@TestPropertySource(properties = {
        "app.api.version-prefix=/api/v1",
        "app.jwt.secret=0123456789abcdef0123456789abcdef",
        "app.jwt.issuer=noura-test",
        "spring.main.allow-bean-definition-overriding=true"
})
class AdminAuthorizationControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AdminAuthorizationController.class)
    static class TestApplication {
    }

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AdminRoleManagementService adminRoleManagementService;

    @Test
    void roles_shouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/authorization/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    @Test
    void roles_shouldRejectManagerRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/authorization/roles")
                        .with(user("manager@noura.test").roles("MANAGER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void permissions_shouldAllowAdminRole() throws Exception {
        when(adminRoleManagementService.listPermissions()).thenReturn(List.of(
                new AdminPermissionDto(
                        UUID.randomUUID(),
                        "products",
                        "read",
                        "Products Read",
                        "Read products",
                        "commerce",
                        100,
                        false
                )
        ));

        mockMvc.perform(get("/api/v1/admin/authorization/permissions")
                        .with(user("admin@noura.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scope").value("products"))
                .andExpect(jsonPath("$.data[0].action").value("read"));
    }

    @Test
    void permissions_shouldAllowRolesReadAuthority() throws Exception {
        when(adminRoleManagementService.listPermissions()).thenReturn(List.of(
                new AdminPermissionDto(
                        UUID.randomUUID(),
                        "roles",
                        "read",
                        "Roles Read",
                        "Read roles",
                        "governance",
                        200,
                        true
                )
        ));

        mockMvc.perform(get("/api/v1/admin/authorization/permissions")
                        .with(user("rbac.viewer@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_ROLES_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scope").value("roles"));
    }

    @Test
    void permissionPresets_shouldAllowRolesReadAuthority() throws Exception {
        when(adminRoleManagementService.listPermissionPresets()).thenReturn(List.of(
                new AdminPermissionPresetDto(
                        "ORDER_MANAGER",
                        "Order Manager",
                        "Order operations preset",
                        2,
                        6,
                        Map.of("orders", List.of("read", "update", "approve"))
                )
        ));

        mockMvc.perform(get("/api/v1/admin/authorization/permission-presets")
                        .with(user("rbac.viewer@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_ROLES_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("ORDER_MANAGER"));
    }

    @Test
    void createRole_shouldAllowSuperAdminRole() throws Exception {
        UUID roleId = UUID.randomUUID();
        when(adminRoleManagementService.createRole(any())).thenReturn(
                new AdminRolePermissionDto(
                        roleId,
                        "OPERATIONS_LEAD",
                        "Operations Lead",
                        "Operations role",
                        false,
                        true,
                        true,
                        0L,
                        Map.of("orders", List.of("read", "update")),
                        List.of("commerce.orders")
                )
        );

        mockMvc.perform(post("/api/v1/admin/authorization/roles")
                        .with(user("super@noura.test").roles("SUPER_ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "OPERATIONS_LEAD",
                                "label", "Operations Lead",
                                "description", "Operations role",
                                "assignable", true,
                                "active", true,
                                "grants", Map.of("orders", List.of("read", "update"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(roleId.toString()))
                .andExpect(jsonPath("$.data.role").value("OPERATIONS_LEAD"));
    }

    @Test
    void createRole_shouldRejectReadOnlyPermissionAuthority() throws Exception {
        mockMvc.perform(post("/api/v1/admin/authorization/roles")
                        .with(user("readonly@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_ROLES_READ")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "OPERATIONS_LEAD",
                                "label", "Operations Lead",
                                "grants", Map.of()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void replaceUserRoles_shouldAllowAdminRole() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminRoleManagementService.replaceUserRoleAssignments(any(), any())).thenReturn(
                new AdminUserRoleAssignmentDto(
                        userId,
                        "ops@noura.test",
                        "Ops User",
                        List.of("ORDER_MANAGER"),
                        List.of("ADMIN")
                )
        );

        mockMvc.perform(put("/api/v1/admin/authorization/users/{userId}/roles", userId)
                        .with(user("admin@noura.test").roles("ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", List.of("ORDER_MANAGER")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.adminRoleCodes[0]").value("ORDER_MANAGER"));
    }

    @Test
    void applyPermissionPreset_shouldAllowRolesApproveAuthority() throws Exception {
        UUID roleId = UUID.randomUUID();
        when(adminRoleManagementService.applyPermissionPreset(any(), anyString())).thenReturn(
                new AdminRolePermissionDto(
                        roleId,
                        "OPERATIONS_LEAD",
                        "Operations Lead",
                        "Operations role",
                        false,
                        true,
                        true,
                        0L,
                        Map.of("orders", List.of("read", "update", "approve")),
                        List.of("commerce.orders")
                )
        );

        mockMvc.perform(put("/api/v1/admin/authorization/roles/{roleId}/permission-presets/{presetCode}", roleId, "ORDER_MANAGER")
                        .with(user("rbac.approver@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_ROLES_APPROVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(roleId.toString()))
                .andExpect(jsonPath("$.data.role").value("OPERATIONS_LEAD"));
    }

    @Test
    void previewBulkUserRoles_shouldAllowRolesUpdateAndUsersUpdateAuthorities() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminRoleManagementService.previewBulkUserRoleAssignments(any())).thenReturn(
                new AdminBulkUserRoleAssignmentPreviewDto(
                        1,
                        1,
                        0,
                        1,
                        List.of(),
                        List.of(
                                new AdminBulkUserRoleAssignmentPreviewItemDto(
                                        userId,
                                        "ops1@noura.test",
                                        "Ops One",
                                        List.of(),
                                        List.of("ORDER_MANAGER"),
                                        List.of("ORDER_MANAGER"),
                                        List.of(),
                                        true
                                )
                        )
                )
        );

        mockMvc.perform(post("/api/v1/admin/authorization/users/roles/bulk/preview")
                        .with(user("rbac.manager@noura.test")
                                .authorities(
                                        new SimpleGrantedAuthority("PERM_ROLES_UPDATE"),
                                        new SimpleGrantedAuthority("PERM_USERS_UPDATE")
                                ))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userIds", List.of(userId),
                                "roleCodes", List.of("ORDER_MANAGER")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedUsers").value(1))
                .andExpect(jsonPath("$.data.changedUsers").value(1))
                .andExpect(jsonPath("$.data.items[0].userId").value(userId.toString()));
    }

    @Test
    void previewBulkUserRoles_shouldRejectRolesUpdateWithoutUsersUpdate() throws Exception {
        mockMvc.perform(post("/api/v1/admin/authorization/users/roles/bulk/preview")
                        .with(user("rbac.manager@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_ROLES_UPDATE")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userIds", List.of(UUID.randomUUID()),
                                "roleCodes", List.of("ORDER_MANAGER")
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void listBulkUserRoleViews_shouldAllowAdminRole() throws Exception {
        UUID viewId = UUID.randomUUID();
        when(adminRoleManagementService.listBulkUserRoleViews()).thenReturn(
                List.of(
                        new AdminBulkUserRoleViewDto(
                                viewId,
                                "Ops APAC",
                                "ops",
                                List.of(UUID.randomUUID()),
                                List.of("ORDER_MANAGER"),
                                Instant.parse("2026-03-13T00:00:00Z")
                        )
                )
        );

        mockMvc.perform(get("/api/v1/admin/authorization/users/roles/bulk/views")
                        .with(user("admin@noura.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(viewId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Ops APAC"));
    }

    @Test
    void upsertBulkUserRoleView_shouldAllowRolesUpdateAndUsersUpdateAuthorities() throws Exception {
        UUID viewId = UUID.randomUUID();
        when(adminRoleManagementService.upsertBulkUserRoleView(any())).thenReturn(
                new AdminBulkUserRoleViewDto(
                        viewId,
                        "Ops APAC",
                        "ops",
                        List.of(UUID.randomUUID()),
                        List.of("ORDER_MANAGER"),
                        Instant.parse("2026-03-13T00:00:00Z")
                )
        );

        mockMvc.perform(post("/api/v1/admin/authorization/users/roles/bulk/views")
                        .with(user("rbac.manager@noura.test")
                                .authorities(
                                        new SimpleGrantedAuthority("PERM_ROLES_UPDATE"),
                                        new SimpleGrantedAuthority("PERM_USERS_UPDATE")
                                ))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Ops APAC",
                                "query", "ops",
                                "userIds", List.of(UUID.randomUUID()),
                                "roleCodes", List.of("ORDER_MANAGER")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(viewId.toString()))
                .andExpect(jsonPath("$.data.name").value("Ops APAC"));
    }

    @Test
    void deleteBulkUserRoleView_shouldRejectRolesUpdateWithoutUsersUpdate() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/authorization/users/roles/bulk/views/{viewId}", UUID.randomUUID())
                        .with(user("rbac.manager@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_ROLES_UPDATE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void bulkReplaceUserRoles_shouldAllowAdminRole() throws Exception {
        when(adminRoleManagementService.bulkReplaceUserRoleAssignments(any())).thenReturn(
                new AdminBulkUserRoleAssignmentResultDto(
                        2,
                        2,
                        List.of(
                                new AdminUserRoleAssignmentDto(
                                        UUID.randomUUID(),
                                        "ops1@noura.test",
                                        "Ops One",
                                        List.of("ORDER_MANAGER"),
                                        List.of("ADMIN")
                                ),
                                new AdminUserRoleAssignmentDto(
                                        UUID.randomUUID(),
                                        "ops2@noura.test",
                                        "Ops Two",
                                        List.of("ORDER_MANAGER"),
                                        List.of("ADMIN")
                                )
                        )
                )
        );

        mockMvc.perform(put("/api/v1/admin/authorization/users/roles/bulk")
                        .with(user("admin@noura.test").roles("ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userIds", List.of(UUID.randomUUID(), UUID.randomUUID()),
                                "roleCodes", List.of("ORDER_MANAGER")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedUsers").value(2))
                .andExpect(jsonPath("$.data.updatedUsers").value(2));
    }

    @Test
    void bulkReplaceUserRoles_shouldRejectRolesUpdateWithoutUsersUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/admin/authorization/users/roles/bulk")
                        .with(user("rbac.manager@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_ROLES_UPDATE")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userIds", List.of(UUID.randomUUID()),
                                "roleCodes", List.of("ORDER_MANAGER")
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void auditLogs_shouldAllowAuditReadAuthority() throws Exception {
        UUID logId = UUID.randomUUID();
        when(adminRoleManagementService.listAuditLogs(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(
                new PageImpl<>(
                        List.of(new AdminRbacAuditLogDto(
                                logId,
                                "ROLE_UPDATED",
                                "ROLE",
                                UUID.randomUUID().toString(),
                                "admin@noura.test",
                                UUID.randomUUID(),
                                "SUCCESS",
                                "corr-1",
                                "abcdef123456",
                                "{\"label\":\"Manager\"}",
                                Instant.parse("2026-03-13T00:00:00Z")
                        )),
                        PageRequest.of(0, 20),
                        1
                )
        );

        mockMvc.perform(get("/api/v1/admin/authorization/audit-logs")
                        .with(user("auditor@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_AUDIT_LOGS_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(logId.toString()))
                .andExpect(jsonPath("$.data.content[0].actionType").value("ROLE_UPDATED"));
    }

    @Test
    void exportAuditLogs_shouldAllowAuditExportAuthority() throws Exception {
        when(adminRoleManagementService.exportAuditLogsCsv(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("occurred_at,action_type\n".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/v1/admin/authorization/audit-logs/export")
                        .with(user("auditor@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_AUDIT_LOGS_EXPORT"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("occurred_at")));
    }

    @Test
    void exportAuditLogs_shouldRejectReadOnlyAuditPermission() throws Exception {
        mockMvc.perform(get("/api/v1/admin/authorization/audit-logs/export")
                        .with(user("auditor@noura.test")
                                .authorities(new SimpleGrantedAuthority("PERM_AUDIT_LOGS_READ"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }
}
