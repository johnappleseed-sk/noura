package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentRequest;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentPreviewDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentResultDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleViewDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleViewUpsertRequest;
import com.noura.platform.dto.admin.AdminPermissionDto;
import com.noura.platform.dto.admin.AdminPermissionPresetDto;
import com.noura.platform.dto.admin.AdminRbacAuditLogDto;
import com.noura.platform.dto.admin.AdminRoleCreateRequest;
import com.noura.platform.dto.admin.AdminRolePermissionDto;
import com.noura.platform.dto.admin.AdminRolePermissionUpdateRequest;
import com.noura.platform.dto.admin.AdminRoleUpdateRequest;
import com.noura.platform.dto.admin.AdminUserRoleAssignmentDto;
import com.noura.platform.dto.admin.AdminUserRoleAssignmentRequest;
import com.noura.platform.service.AdminRoleManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Exposes role CRUD and permission assignment APIs for admin governance workflows.
 * Responsibilities:
 * - Manage persisted admin role metadata.
 * - Replace role grants (direct and preset-driven).
 * - Replace user role assignments (single and bulk).
 * - Expose permission catalog for admin UI configuration.
 * Related modules:
 * - AdminRoleManagementService
 * - frontend RolesPermissionsPage
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin/authorization")
public class AdminAuthorizationController {

    private final AdminRoleManagementService adminRoleManagementService;

    /**
     * Lists permission catalog entries.
     *
     * @param http Current request for response metadata.
     * @return Permission catalog payload.
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERM_ROLES_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<AdminPermissionDto>> permissions(HttpServletRequest http) {
        return ApiResponse.ok("Admin permissions", adminRoleManagementService.listPermissions(), http.getRequestURI());
    }

    /**
     * Lists reusable permission presets for role grant workflows.
     *
     * @param http Current request for response metadata.
     * @return Permission preset payload.
     */
    @GetMapping("/permission-presets")
    @PreAuthorize("hasAuthority('PERM_ROLES_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<AdminPermissionPresetDto>> permissionPresets(HttpServletRequest http) {
        return ApiResponse.ok("Permission presets", adminRoleManagementService.listPermissionPresets(), http.getRequestURI());
    }

    /**
     * Lists roles with grants and capabilities.
     *
     * @param http Current request for response metadata.
     * @return Role list payload.
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('PERM_ROLES_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<AdminRolePermissionDto>> roles(HttpServletRequest http) {
        return ApiResponse.ok("Admin roles", adminRoleManagementService.listRoles(), http.getRequestURI());
    }

    /**
     * Creates a new role.
     *
     * @param request Role creation payload.
     * @param http Current request for response metadata.
     * @return Created role payload.
     */
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('PERM_ROLES_CREATE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<AdminRolePermissionDto> createRole(
            @Valid @RequestBody AdminRoleCreateRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Role created", adminRoleManagementService.createRole(request), http.getRequestURI());
    }

    /**
     * Updates role metadata.
     *
     * @param roleId Target role identifier.
     * @param request Update payload.
     * @param http Current request for response metadata.
     * @return Updated role payload.
     */
    @PatchMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('PERM_ROLES_UPDATE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<AdminRolePermissionDto> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody AdminRoleUpdateRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Role updated", adminRoleManagementService.updateRole(roleId, request), http.getRequestURI());
    }

    /**
     * Replaces grants for a target role.
     *
     * @param roleId Target role identifier.
     * @param request Grant map payload.
     * @param http Current request for response metadata.
     * @return Updated role payload.
     */
    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("(hasAuthority('PERM_ROLES_UPDATE') or hasAuthority('PERM_ROLES_APPROVE')) or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<AdminRolePermissionDto> replaceRolePermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody AdminRolePermissionUpdateRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Role permissions updated",
                adminRoleManagementService.replaceRolePermissions(roleId, request),
                http.getRequestURI()
        );
    }

    /**
     * Applies a permission preset onto a target role.
     *
     * @param roleId Target role identifier.
     * @param presetCode Preset role/policy code.
     * @param http Current request for response metadata.
     * @return Updated role payload.
     */
    @PutMapping("/roles/{roleId}/permission-presets/{presetCode}")
    @PreAuthorize("(hasAuthority('PERM_ROLES_UPDATE') or hasAuthority('PERM_ROLES_APPROVE')) or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<AdminRolePermissionDto> applyPermissionPreset(
            @PathVariable UUID roleId,
            @PathVariable String presetCode,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Role permission preset applied",
                adminRoleManagementService.applyPermissionPreset(roleId, presetCode),
                http.getRequestURI()
        );
    }

    /**
     * Deactivates a non-system role.
     *
     * @param roleId Target role identifier.
     * @param http Current request for response metadata.
     * @return Empty success response.
     */
    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('PERM_ROLES_DELETE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> deactivateRole(@PathVariable UUID roleId, HttpServletRequest http) {
        adminRoleManagementService.deactivateRole(roleId);
        return ApiResponse.ok("Role deactivated", null, http.getRequestURI());
    }

    /**
     * Retrieves role assignments for a target user.
     *
     * @param userId Target user identifier.
     * @param http Current request for response metadata.
     * @return User assignment payload.
     */
    @GetMapping("/users/{userId}/roles")
    @PreAuthorize("((hasAuthority('PERM_ROLES_READ') and hasAuthority('PERM_USERS_READ')) or hasAnyRole('ADMIN','SUPER_ADMIN'))")
    public ApiResponse<AdminUserRoleAssignmentDto> userRoleAssignments(@PathVariable UUID userId, HttpServletRequest http) {
        return ApiResponse.ok(
                "User role assignments",
                adminRoleManagementService.getUserRoleAssignments(userId),
                http.getRequestURI()
        );
    }

    /**
     * Replaces role assignments for a target user.
     *
     * @param userId Target user identifier.
     * @param request Assignment payload.
     * @param http Current request for response metadata.
     * @return Updated user assignment payload.
     */
    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("(((hasAuthority('PERM_ROLES_UPDATE') or hasAuthority('PERM_ROLES_APPROVE')) and hasAuthority('PERM_USERS_UPDATE')) or hasAnyRole('ADMIN','SUPER_ADMIN'))")
    public ApiResponse<AdminUserRoleAssignmentDto> replaceUserRoleAssignments(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserRoleAssignmentRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "User role assignments updated",
                adminRoleManagementService.replaceUserRoleAssignments(userId, request),
                http.getRequestURI()
        );
    }

    /**
     * Previews role-assignment deltas for multiple users.
     *
     * @param request Bulk assignment payload.
     * @param http Current request for response metadata.
     * @return Preview payload with per-user add/remove deltas.
     */
    @PostMapping("/users/roles/bulk/preview")
    @PreAuthorize("(((hasAuthority('PERM_ROLES_UPDATE') or hasAuthority('PERM_ROLES_APPROVE')) and hasAuthority('PERM_USERS_UPDATE')) or hasAnyRole('ADMIN','SUPER_ADMIN'))")
    public ApiResponse<AdminBulkUserRoleAssignmentPreviewDto> previewBulkUserRoleAssignments(
            @Valid @RequestBody AdminBulkUserRoleAssignmentRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Bulk user role assignment preview",
                adminRoleManagementService.previewBulkUserRoleAssignments(request),
                http.getRequestURI()
        );
    }

    /**
     * Replaces role assignments for multiple users in one operation.
     *
     * @param request Bulk assignment payload.
     * @param http Current request for response metadata.
     * @return Bulk assignment summary payload.
     */
    @PutMapping("/users/roles/bulk")
    @PreAuthorize("(((hasAuthority('PERM_ROLES_UPDATE') or hasAuthority('PERM_ROLES_APPROVE')) and hasAuthority('PERM_USERS_UPDATE')) or hasAnyRole('ADMIN','SUPER_ADMIN'))")
    public ApiResponse<AdminBulkUserRoleAssignmentResultDto> bulkReplaceUserRoleAssignments(
            @Valid @RequestBody AdminBulkUserRoleAssignmentRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Bulk user role assignments updated",
                adminRoleManagementService.bulkReplaceUserRoleAssignments(request),
                http.getRequestURI()
        );
    }

    /**
     * Lists saved bulk-assignment views for the current actor.
     *
     * @param http Current request for response metadata.
     * @return Saved view payload.
     */
    @GetMapping("/users/roles/bulk/views")
    @PreAuthorize("(((hasAuthority('PERM_ROLES_UPDATE') or hasAuthority('PERM_ROLES_APPROVE')) and hasAuthority('PERM_USERS_UPDATE')) or hasAnyRole('ADMIN','SUPER_ADMIN'))")
    public ApiResponse<List<AdminBulkUserRoleViewDto>> listBulkUserRoleViews(HttpServletRequest http) {
        return ApiResponse.ok(
                "Bulk user role assignment views",
                adminRoleManagementService.listBulkUserRoleViews(),
                http.getRequestURI()
        );
    }

    /**
     * Creates or updates a saved bulk-assignment view for the current actor.
     *
     * @param request Saved-view payload.
     * @param http Current request for response metadata.
     * @return Saved view payload.
     */
    @PostMapping("/users/roles/bulk/views")
    @PreAuthorize("(((hasAuthority('PERM_ROLES_UPDATE') or hasAuthority('PERM_ROLES_APPROVE')) and hasAuthority('PERM_USERS_UPDATE')) or hasAnyRole('ADMIN','SUPER_ADMIN'))")
    public ApiResponse<AdminBulkUserRoleViewDto> upsertBulkUserRoleView(
            @Valid @RequestBody AdminBulkUserRoleViewUpsertRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Bulk user role assignment view saved",
                adminRoleManagementService.upsertBulkUserRoleView(request),
                http.getRequestURI()
        );
    }

    /**
     * Deletes a saved bulk-assignment view owned by the current actor.
     *
     * @param viewId Target view identifier.
     * @param http Current request for response metadata.
     * @return Empty success response.
     */
    @DeleteMapping("/users/roles/bulk/views/{viewId}")
    @PreAuthorize("(((hasAuthority('PERM_ROLES_UPDATE') or hasAuthority('PERM_ROLES_APPROVE')) and hasAuthority('PERM_USERS_UPDATE')) or hasAnyRole('ADMIN','SUPER_ADMIN'))")
    public ApiResponse<Void> deleteBulkUserRoleView(@PathVariable UUID viewId, HttpServletRequest http) {
        adminRoleManagementService.deleteBulkUserRoleView(viewId);
        return ApiResponse.ok("Bulk user role assignment view deleted", null, http.getRequestURI());
    }

    /**
     * Lists RBAC governance audit-log entries.
     *
     * @param actionType Optional action type filter.
     * @param entityType Optional entity type filter.
     * @param outcome Optional outcome filter.
     * @param query Optional free-text query.
     * @param errorsOnly Whether to only include non-successful outcomes.
     * @param occurredFrom Optional lower timestamp bound.
     * @param occurredTo Optional upper timestamp bound.
     * @param page The pagination page index.
     * @param size The pagination page size.
     * @param sortBy The sort field.
     * @param direction The sort direction.
     * @param http Current request for response metadata.
     * @return Paginated audit-log payload.
     */
    @GetMapping("/audit-logs")
    @PreAuthorize("(hasAuthority('PERM_AUDIT_LOGS_READ') or hasAuthority('PERM_ROLES_READ')) or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<PageResponse<AdminRbacAuditLogDto>> auditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean errorsOnly,
            @RequestParam(required = false) Instant occurredFrom,
            @RequestParam(required = false) Instant occurredTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<AdminRbacAuditLogDto> logs = adminRoleManagementService.listAuditLogs(
                actionType,
                entityType,
                outcome,
                query,
                errorsOnly,
                occurredFrom,
                occurredTo,
                pageable
        );
        return ApiResponse.ok("Authorization audit logs", PageResponse.from(logs), http.getRequestURI());
    }

    /**
     * Exports RBAC governance audit logs as CSV.
     *
     * @param actionType Optional action type filter.
     * @param entityType Optional entity type filter.
     * @param outcome Optional outcome filter.
     * @param query Optional free-text query.
     * @param errorsOnly Whether to only include non-successful outcomes.
     * @param occurredFrom Optional lower timestamp bound.
     * @param occurredTo Optional upper timestamp bound.
     * @return CSV download response.
     */
    @GetMapping("/audit-logs/export")
    @PreAuthorize("(hasAuthority('PERM_AUDIT_LOGS_EXPORT') or hasAnyRole('ADMIN','SUPER_ADMIN'))")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean errorsOnly,
            @RequestParam(required = false) Instant occurredFrom,
            @RequestParam(required = false) Instant occurredTo
    ) {
        byte[] content = adminRoleManagementService.exportAuditLogsCsv(
                actionType,
                entityType,
                outcome,
                query,
                errorsOnly,
                occurredFrom,
                occurredTo
        );
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("admin-rbac-audit-logs.csv")
                        .build().toString())
                .body(content);
    }
}
