package com.noura.platform.service;

import com.noura.platform.dto.admin.AdminPermissionDto;
import com.noura.platform.dto.admin.AdminPermissionPresetDto;
import com.noura.platform.dto.admin.AdminRbacAuditLogDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentRequest;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentPreviewDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentResultDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleViewDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleViewUpsertRequest;
import com.noura.platform.dto.admin.AdminRoleCreateRequest;
import com.noura.platform.dto.admin.AdminRolePermissionDto;
import com.noura.platform.dto.admin.AdminRolePermissionUpdateRequest;
import com.noura.platform.dto.admin.AdminRoleUpdateRequest;
import com.noura.platform.dto.admin.AdminUserRoleAssignmentDto;
import com.noura.platform.dto.admin.AdminUserRoleAssignmentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Contract for role CRUD and permission assignment workflows.
 * Responsibilities:
 * - Manage admin roles, role grants, and preset-based grant workflows.
 * - Manage user-to-admin-role assignments (single and bulk).
 * - Expose permission catalog for admin UI workflows.
 * Related modules:
 * - AdminAuthorizationController
 * - AdminAuthorizationService
 */
public interface AdminRoleManagementService {

    List<AdminPermissionDto> listPermissions();

    List<AdminPermissionPresetDto> listPermissionPresets();

    List<AdminRolePermissionDto> listRoles();

    AdminRolePermissionDto createRole(AdminRoleCreateRequest request);

    AdminRolePermissionDto updateRole(UUID roleId, AdminRoleUpdateRequest request);

    void deactivateRole(UUID roleId);

    AdminRolePermissionDto replaceRolePermissions(UUID roleId, AdminRolePermissionUpdateRequest request);

    AdminRolePermissionDto applyPermissionPreset(UUID roleId, String presetCode);

    AdminUserRoleAssignmentDto getUserRoleAssignments(UUID userId);

    AdminUserRoleAssignmentDto replaceUserRoleAssignments(UUID userId, AdminUserRoleAssignmentRequest request);

    AdminBulkUserRoleAssignmentPreviewDto previewBulkUserRoleAssignments(AdminBulkUserRoleAssignmentRequest request);

    AdminBulkUserRoleAssignmentResultDto bulkReplaceUserRoleAssignments(AdminBulkUserRoleAssignmentRequest request);

    List<AdminBulkUserRoleViewDto> listBulkUserRoleViews();

    AdminBulkUserRoleViewDto upsertBulkUserRoleView(AdminBulkUserRoleViewUpsertRequest request);

    void deleteBulkUserRoleView(UUID viewId);

    Page<AdminRbacAuditLogDto> listAuditLogs(
            String actionType,
            String entityType,
            String outcome,
            String query,
            Boolean errorsOnly,
            Instant occurredFrom,
            Instant occurredTo,
            Pageable pageable
    );

    byte[] exportAuditLogsCsv(
            String actionType,
            String entityType,
            String outcome,
            String query,
            Boolean errorsOnly,
            Instant occurredFrom,
            Instant occurredTo
    );
}
