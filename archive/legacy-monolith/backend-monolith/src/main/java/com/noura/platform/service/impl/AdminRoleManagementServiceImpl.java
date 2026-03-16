package com.noura.platform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.AdminBulkUserRoleView;
import com.noura.platform.domain.entity.AdminRbacAuditLog;
import com.noura.platform.domain.entity.AdminPermission;
import com.noura.platform.domain.entity.AdminRole;
import com.noura.platform.domain.entity.AdminRolePermission;
import com.noura.platform.domain.entity.AdminUserRole;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.entity.id.AdminRolePermissionId;
import com.noura.platform.domain.entity.id.AdminUserRoleId;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentRequest;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentPreviewDto;
import com.noura.platform.dto.admin.AdminBulkUserRoleAssignmentPreviewItemDto;
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
import com.noura.platform.repository.AdminBulkUserRoleViewRepository;
import com.noura.platform.repository.AdminPermissionRepository;
import com.noura.platform.repository.AdminRbacAuditLogRepository;
import com.noura.platform.repository.AdminRolePermissionRepository;
import com.noura.platform.repository.AdminRoleRepository;
import com.noura.platform.repository.AdminUserRoleRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.projection.AdminRoleAssignmentCountProjection;
import com.noura.platform.service.AdminAuthorizationService;
import com.noura.platform.service.AdminRoleManagementService;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Module: Admin Authorization
 * Purpose: Implements persistent role CRUD and permission assignment workflows.
 * Responsibilities:
 * - Manage admin role metadata and lifecycle.
 * - Replace role grants and apply permission presets.
 * - Replace user role assignments (single and bulk).
 * - Persist actor-scoped bulk assignment saved views.
 * - Expose role and permission catalog views for admin UI management.
 * Related modules:
 * - AdminAuthorizationController
 * - AdminAuthorizationService
 * - AdminRoleRepository/AdminPermissionRepository
 */
@Service
@RequiredArgsConstructor
public class AdminRoleManagementServiceImpl implements AdminRoleManagementService {

    private final AdminRoleRepository adminRoleRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final AdminRolePermissionRepository adminRolePermissionRepository;
    private final AdminUserRoleRepository adminUserRoleRepository;
    private final AdminBulkUserRoleViewRepository adminBulkUserRoleViewRepository;
    private final AdminRbacAuditLogRepository adminRbacAuditLogRepository;
    private final UserAccountRepository userAccountRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final ObjectMapper objectMapper;

    /**
     * Lists the full persisted permission catalog.
     *
     * @return Ordered permission entries.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdminPermissionDto> listPermissions() {
        return adminPermissionRepository.findAllByOrderByDisplayOrderAscScopeAscActionAsc().stream()
                .map(permission -> new AdminPermissionDto(
                        permission.getId(),
                        permission.getScope(),
                        permission.getAction(),
                        permission.getLabel(),
                        permission.getDescription(),
                        permission.getModuleGroup(),
                        permission.getDisplayOrder(),
                        permission.isSensitive()
                ))
                .toList();
    }

    /**
     * Lists reusable permission presets derived from active system roles.
     *
     * @return Preset metadata and grants for UI preset picker workflows.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdminPermissionPresetDto> listPermissionPresets() {
        List<AdminRole> presetRoles = adminRoleRepository.findAllBySystemRoleTrueAndActiveTrueOrderByCodeAsc();
        return mapRoles(presetRoles).stream()
                .map(role -> new AdminPermissionPresetDto(
                        role.role(),
                        role.label(),
                        role.description(),
                        role.grants() == null ? 0 : role.grants().size(),
                        countPermissions(role.grants()),
                        role.grants() == null ? Map.of() : role.grants()
                ))
                .toList();
    }

    /**
     * Lists all roles with grants, capabilities, and assignment counts.
     *
     * @return Ordered role records for management UI.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdminRolePermissionDto> listRoles() {
        List<AdminRole> roles = adminRoleRepository.findAllByOrderByCodeAsc();
        return mapRoles(roles);
    }

    /**
     * Lists RBAC governance audit logs with optional filters.
     *
     * @param actionType Optional action type filter.
     * @param entityType Optional entity type filter.
     * @param outcome Optional outcome filter.
     * @param query Optional free-text query filter.
     * @param errorsOnly Whether to only include non-successful outcomes.
     * @param occurredFrom Optional lower timestamp bound.
     * @param occurredTo Optional upper timestamp bound.
     * @param pageable Pagination configuration.
     * @return Page of RBAC audit-log rows.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AdminRbacAuditLogDto> listAuditLogs(
            String actionType,
            String entityType,
            String outcome,
            String query,
            Boolean errorsOnly,
            Instant occurredFrom,
            Instant occurredTo,
            Pageable pageable
    ) {
        Specification<AdminRbacAuditLog> spec = buildAuditLogSpecification(
                actionType,
                entityType,
                outcome,
                query,
                errorsOnly,
                occurredFrom,
                occurredTo
        );
        return adminRbacAuditLogRepository.findAll(spec, pageable)
                .map(this::toAuditLogDto);
    }

    /**
     * Exports RBAC audit logs as CSV using current filter criteria.
     *
     * @param actionType Optional action type filter.
     * @param entityType Optional entity type filter.
     * @param outcome Optional outcome filter.
     * @param query Optional free-text query filter.
     * @param errorsOnly Whether to only include non-successful outcomes.
     * @param occurredFrom Optional lower timestamp bound.
     * @param occurredTo Optional upper timestamp bound.
     * @return CSV bytes encoded as UTF-8.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportAuditLogsCsv(
            String actionType,
            String entityType,
            String outcome,
            String query,
            Boolean errorsOnly,
            Instant occurredFrom,
            Instant occurredTo
    ) {
        Specification<AdminRbacAuditLog> spec = buildAuditLogSpecification(
                actionType,
                entityType,
                outcome,
                query,
                errorsOnly,
                occurredFrom,
                occurredTo
        );
        List<AdminRbacAuditLog> rows = adminRbacAuditLogRepository.findAll(
                spec,
                PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "occurredAt"))
        ).getContent();

        StringBuilder csv = new StringBuilder();
        csv.append("occurred_at,action_type,entity_type,entity_id,actor_email,actor_user_id,outcome,correlation_id,payload_hash,details_json\n");
        for (AdminRbacAuditLog row : rows) {
            csv.append(csv(row.getOccurredAt() == null ? null : row.getOccurredAt().toString())).append(',')
                    .append(csv(row.getActionType())).append(',')
                    .append(csv(row.getEntityType())).append(',')
                    .append(csv(row.getEntityId())).append(',')
                    .append(csv(row.getActorEmail())).append(',')
                    .append(csv(row.getActorUserId() == null ? null : row.getActorUserId().toString())).append(',')
                    .append(csv(row.getOutcome())).append(',')
                    .append(csv(row.getCorrelationId())).append(',')
                    .append(csv(row.getPayloadHash())).append(',')
                    .append(csv(row.getDetailsJson()))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Specification<AdminRbacAuditLog> buildAuditLogSpecification(
            String actionType,
            String entityType,
            String outcome,
            String query,
            Boolean errorsOnly,
            Instant occurredFrom,
            Instant occurredTo
    ) {
        Specification<AdminRbacAuditLog> spec = Specification.where(null);

        if (actionType != null && !actionType.isBlank()) {
            String normalizedAction = actionType.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, ignoredQuery, cb) -> cb.equal(cb.lower(root.get("actionType")), normalizedAction));
        }
        if (entityType != null && !entityType.isBlank()) {
            String normalizedEntityType = entityType.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, ignoredQuery, cb) -> cb.equal(cb.lower(root.get("entityType")), normalizedEntityType));
        }
        if (outcome != null && !outcome.isBlank()) {
            String normalizedOutcome = outcome.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, ignoredQuery, cb) -> cb.equal(cb.lower(root.get("outcome")), normalizedOutcome));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, ignoredQuery, cb) -> cb.or(
                    cb.like(cb.lower(root.get("actionType")), like),
                    cb.like(cb.lower(root.get("entityType")), like),
                    cb.like(cb.lower(root.get("entityId")), like),
                    cb.like(cb.lower(root.get("actorEmail")), like),
                    cb.like(cb.lower(root.get("outcome")), like),
                    cb.like(cb.lower(root.get("correlationId")), like),
                    cb.like(cb.lower(root.get("payloadHash")), like),
                    cb.like(cb.lower(root.get("detailsJson")), like)
            ));
        }
        if (Boolean.TRUE.equals(errorsOnly)) {
            spec = spec.and((root, ignoredQuery, cb) -> cb.notEqual(cb.lower(root.get("outcome")), "success"));
        }
        if (occurredFrom != null) {
            spec = spec.and((root, ignoredQuery, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), occurredFrom));
        }
        if (occurredTo != null) {
            spec = spec.and((root, ignoredQuery, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), occurredTo));
        }
        return spec;
    }

    /**
     * Creates a new admin role and optional initial grants.
     *
     * @param request Role creation payload.
     * @return Persisted role record.
     */
    @Override
    @Transactional
    public AdminRolePermissionDto createRole(AdminRoleCreateRequest request) {
        String code = normalizeRoleCode(request.code());
        if (adminRoleRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("ROLE_EXISTS", "Role code already exists");
        }

        AdminRole role = new AdminRole();
        role.setCode(code);
        role.setLabel(normalizeRequiredLabel(request.label()));
        role.setDescription(normalizeOptionalText(request.description()));
        role.setAssignable(request.assignable() == null || request.assignable());
        role.setActive(request.active() == null || request.active());
        role.setSystemRole(false);
        role = adminRoleRepository.save(role);

        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("roleCode", role.getCode());
        details.put("label", role.getLabel());
        details.put("assignable", role.isAssignable());
        details.put("active", role.isActive());
        details.put("requestedGrants", request.grants());
        recordAuditLog("ROLE_CREATED", "ROLE", role.getId(), details);

        if (request.grants() != null && !request.grants().isEmpty()) {
            return replaceRolePermissions(role.getId(), new AdminRolePermissionUpdateRequest(request.grants()));
        }

        return mapSingleRole(role);
    }

    /**
     * Updates mutable role metadata.
     *
     * @param roleId Role identifier.
     * @param request Update payload.
     * @return Updated role record.
     */
    @Override
    @Transactional
    public AdminRolePermissionDto updateRole(UUID roleId, AdminRoleUpdateRequest request) {
        AdminRole role = requireRole(roleId);

        if (request.label() != null) {
            role.setLabel(normalizeRequiredLabel(request.label()));
        }
        if (request.description() != null) {
            role.setDescription(normalizeOptionalText(request.description()));
        }
        if (request.assignable() != null) {
            if (role.isSystemRole()) {
                throw new BadRequestException("SYSTEM_ROLE_IMMUTABLE", "System role assignability cannot be modified");
            }
            role.setAssignable(request.assignable());
        }
        if (request.active() != null) {
            if (role.isSystemRole() && !request.active()) {
                throw new BadRequestException("SYSTEM_ROLE_IMMUTABLE", "System role cannot be deactivated");
            }
            role.setActive(request.active());
        }

        AdminRole saved = adminRoleRepository.save(role);
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("roleCode", saved.getCode());
        details.put("label", saved.getLabel());
        details.put("description", saved.getDescription());
        details.put("assignable", saved.isAssignable());
        details.put("active", saved.isActive());
        recordAuditLog("ROLE_UPDATED", "ROLE", saved.getId(), details);
        return mapSingleRole(saved);
    }

    /**
     * Soft-deactivates a non-system role.
     *
     * @param roleId Role identifier.
     */
    @Override
    @Transactional
    public void deactivateRole(UUID roleId) {
        AdminRole role = requireRole(roleId);
        if (role.isSystemRole()) {
            throw new BadRequestException("SYSTEM_ROLE_IMMUTABLE", "System role cannot be deactivated");
        }
        role.setActive(false);
        adminRoleRepository.save(role);
        adminUserRoleRepository.deleteByRoleId(roleId);

        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("roleCode", role.getCode());
        details.put("assignable", role.isAssignable());
        details.put("active", role.isActive());
        recordAuditLog("ROLE_DEACTIVATED", "ROLE", roleId, details);
    }

    /**
     * Replaces all grants assigned to a role using scope-action map payload.
     *
     * @param roleId Role identifier.
     * @param request Grant map payload.
     * @return Updated role record.
     */
    @Override
    @Transactional
    public AdminRolePermissionDto replaceRolePermissions(UUID roleId, AdminRolePermissionUpdateRequest request) {
        AdminRole role = requireRole(roleId);
        Set<AdminPermission> permissions = resolvePermissions(request.grants());
        replaceRolePermissionsInternal(role, permissions);

        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("roleCode", role.getCode());
        details.put("grants", request.grants());
        details.put("permissionCount", permissions.size());
        recordAuditLog("ROLE_PERMISSIONS_REPLACED", "ROLE", roleId, details);

        return mapSingleRole(role);
    }

    /**
     * Applies a reusable preset policy onto a target role.
     *
     * @param roleId Target role identifier.
     * @param presetCode Preset code derived from active system roles.
     * @return Updated role record.
     */
    @Override
    @Transactional
    public AdminRolePermissionDto applyPermissionPreset(UUID roleId, String presetCode) {
        AdminRole role = requireRole(roleId);
        AdminRole presetRole = adminRoleRepository.findByCodeIgnoreCase(presetCode)
                .filter(AdminRole::isActive)
                .filter(AdminRole::isSystemRole)
                .orElseThrow(() -> new NotFoundException("PERMISSION_PRESET_NOT_FOUND", "Permission preset not found"));

        Set<AdminPermission> presetPermissions = adminRolePermissionRepository.findDetailedByRoleIds(List.of(presetRole.getId())).stream()
                .map(AdminRolePermission::getPermission)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        replaceRolePermissionsInternal(role, presetPermissions);

        AdminRolePermissionDto presetDto = mapSingleRole(presetRole);
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("roleCode", role.getCode());
        details.put("presetCode", presetRole.getCode());
        details.put("presetLabel", presetRole.getLabel());
        details.put("permissionCount", presetPermissions.size());
        details.put("grants", presetDto.grants());
        recordAuditLog("ROLE_PERMISSION_PRESET_APPLIED", "ROLE", roleId, details);

        return mapSingleRole(role);
    }

    /**
     * Retrieves admin role assignments for a target user.
     *
     * @param userId User identifier.
     * @return Assignment payload.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminUserRoleAssignmentDto getUserRoleAssignments(UUID userId) {
        UserAccount user = requireUser(userId);
        List<String> adminRoleCodes = adminUserRoleRepository.findDetailedByUserId(userId).stream()
                .map(assignment -> assignment.getRole().getCode())
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();

        List<String> platformRoles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream().map(Enum::name).sorted().toList();

        return new AdminUserRoleAssignmentDto(user.getId(), user.getEmail(), user.getFullName(), adminRoleCodes, platformRoles);
    }

    /**
     * Replaces admin role assignments for a target user.
     *
     * @param userId User identifier.
     * @param request Assignment payload.
     * @return Updated assignment payload.
     */
    @Override
    @Transactional
    public AdminUserRoleAssignmentDto replaceUserRoleAssignments(UUID userId, AdminUserRoleAssignmentRequest request) {
        UserAccount user = requireUser(userId);
        List<AdminRole> roles = resolveAssignableRoles(request.roleCodes());
        replaceUserRolesInternal(List.of(user), roles);

        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("userId", userId);
        details.put("userEmail", user.getEmail());
        details.put("roleCodes", roles.stream().map(AdminRole::getCode).sorted().toList());
        recordAuditLog("USER_ROLE_ASSIGNMENTS_REPLACED", "USER", userId, details);

        return getUserRoleAssignments(userId);
    }

    /**
     * Previews per-user role deltas before executing a bulk replacement.
     *
     * @param request Bulk assignment payload.
     * @return Preview payload with conflict details.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminBulkUserRoleAssignmentPreviewDto previewBulkUserRoleAssignments(AdminBulkUserRoleAssignmentRequest request) {
        List<UUID> userIds = normalizeUserIds(request.userIds());
        List<AdminRole> proposedRoles = resolveAssignableRoles(request.roleCodes());
        List<String> proposedRoleCodes = proposedRoles.stream().map(AdminRole::getCode).sorted().toList();
        Set<UUID> requestedUserIds = new LinkedHashSet<>(userIds);

        List<UserAccount> users = userAccountRepository.findAllById(userIds);
        Map<UUID, UserAccount> usersById = users.stream()
                .collect(Collectors.toMap(UserAccount::getId, user -> user, (left, right) -> left));

        List<UUID> missingUserIds = userIds.stream()
                .filter(userId -> !usersById.containsKey(userId))
                .toList();

        List<AdminUserRole> currentAssignments = usersById.isEmpty()
                ? List.of()
                : adminUserRoleRepository.findDetailedByUserIds(usersById.keySet());
        Map<UUID, List<String>> currentRoleCodesByUser = currentAssignments.stream()
                .filter(assignment -> assignment.getUser() != null && assignment.getUser().getId() != null)
                .filter(assignment -> assignment.getRole() != null && assignment.getRole().getCode() != null)
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getUser().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                assignment -> assignment.getRole().getCode().toUpperCase(Locale.ROOT),
                                Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), codes -> codes.stream().sorted().toList())
                        )
                ));

        List<AdminBulkUserRoleAssignmentPreviewItemDto> items = userIds.stream()
                .filter(requestedUserIds::contains)
                .filter(usersById::containsKey)
                .map(userId -> {
                    UserAccount user = usersById.get(userId);
                    List<String> currentRoleCodes = currentRoleCodesByUser.getOrDefault(userId, List.of());
                    Set<String> currentSet = new LinkedHashSet<>(currentRoleCodes);
                    Set<String> proposedSet = new LinkedHashSet<>(proposedRoleCodes);
                    List<String> additions = proposedSet.stream()
                            .filter(code -> !currentSet.contains(code))
                            .sorted()
                            .toList();
                    List<String> removals = currentSet.stream()
                            .filter(code -> !proposedSet.contains(code))
                            .sorted()
                            .toList();
                    boolean changed = !additions.isEmpty() || !removals.isEmpty();
                    return new AdminBulkUserRoleAssignmentPreviewItemDto(
                            user.getId(),
                            user.getEmail(),
                            user.getFullName(),
                            currentRoleCodes,
                            proposedRoleCodes,
                            additions,
                            removals,
                            changed
                    );
                })
                .toList();

        int changedUsers = (int) items.stream().filter(AdminBulkUserRoleAssignmentPreviewItemDto::changed).count();
        return new AdminBulkUserRoleAssignmentPreviewDto(
                userIds.size(),
                items.size(),
                missingUserIds.size(),
                changedUsers,
                missingUserIds,
                items
        );
    }

    /**
     * Replaces role assignments for a bounded list of users.
     *
     * @param request Bulk assignment payload.
     * @return Bulk assignment summary.
     */
    @Override
    @Transactional
    public AdminBulkUserRoleAssignmentResultDto bulkReplaceUserRoleAssignments(AdminBulkUserRoleAssignmentRequest request) {
        List<UUID> userIds = normalizeUserIds(request.userIds());
        List<UserAccount> users = userIds.stream()
                .map(this::requireUser)
                .toList();
        List<AdminRole> roles = resolveAssignableRoles(request.roleCodes());
        List<String> normalizedRoleCodes = roles.stream().map(AdminRole::getCode).sorted().toList();

        replaceUserRolesInternal(users, roles);

        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("userIds", users.stream().map(UserAccount::getId).toList());
        details.put("userCount", users.size());
        details.put("roleCodes", normalizedRoleCodes);
        recordAuditLog("USER_ROLE_ASSIGNMENTS_BULK_REPLACED", "USER_BATCH", null, details);

        List<AdminUserRoleAssignmentDto> assignments = users.stream()
                .map(user -> new AdminUserRoleAssignmentDto(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        normalizedRoleCodes,
                        user.getRoles() == null
                                ? List.of()
                                : user.getRoles().stream().map(Enum::name).sorted().toList()
                ))
                .toList();

        return new AdminBulkUserRoleAssignmentResultDto(
                userIds.size(),
                users.size(),
                assignments
        );
    }

    /**
     * Lists saved bulk-assignment views for the current actor.
     *
     * @return Saved view records.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdminBulkUserRoleViewDto> listBulkUserRoleViews() {
        UUID actorUserId = requireCurrentActorUserId();
        return adminBulkUserRoleViewRepository.findAllByOwnerUserIdOrderByNameAsc(actorUserId).stream()
                .map(this::mapBulkUserRoleView)
                .toList();
    }

    /**
     * Creates or updates a saved bulk-assignment view for the current actor.
     *
     * @param request Saved-view payload.
     * @return Persisted saved-view record.
     */
    @Override
    @Transactional
    public AdminBulkUserRoleViewDto upsertBulkUserRoleView(AdminBulkUserRoleViewUpsertRequest request) {
        UUID actorUserId = requireCurrentActorUserId();
        String normalizedName = normalizeBulkViewName(request.name());
        String normalizedQuery = normalizeOptionalQuery(request.query());
        List<UUID> normalizedUserIds = normalizeOptionalUserIds(request.userIds());
        List<String> normalizedRoleCodes = resolveAssignableRoles(request.roleCodes()).stream()
                .map(AdminRole::getCode)
                .sorted()
                .toList();

        AdminBulkUserRoleView view = adminBulkUserRoleViewRepository
                .findByOwnerUserIdAndNameIgnoreCase(actorUserId, normalizedName)
                .orElseGet(AdminBulkUserRoleView::new);

        view.setOwnerUserId(actorUserId);
        view.setName(normalizedName);
        view.setQueryText(normalizedQuery);
        view.setUserIdsJson(serializeJsonArray(normalizedUserIds));
        view.setRoleCodesJson(serializeJsonArray(normalizedRoleCodes));

        AdminBulkUserRoleView saved = adminBulkUserRoleViewRepository.save(view);

        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("viewId", saved.getId());
        details.put("name", saved.getName());
        details.put("query", saved.getQueryText());
        details.put("selectedUsers", normalizedUserIds.size());
        details.put("selectedRoles", normalizedRoleCodes);
        recordAuditLog("USER_ROLE_BULK_VIEW_UPSERTED", "USER_ROLE_BULK_VIEW", saved.getId(), details);

        return mapBulkUserRoleView(saved);
    }

    /**
     * Deletes a saved bulk-assignment view owned by the current actor.
     *
     * @param viewId Saved-view identifier.
     */
    @Override
    @Transactional
    public void deleteBulkUserRoleView(UUID viewId) {
        UUID actorUserId = requireCurrentActorUserId();
        AdminBulkUserRoleView view = adminBulkUserRoleViewRepository.findByIdAndOwnerUserId(viewId, actorUserId)
                .orElseThrow(() -> new NotFoundException("BULK_VIEW_NOT_FOUND", "Bulk assignment view not found"));
        adminBulkUserRoleViewRepository.delete(view);

        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("viewId", view.getId());
        details.put("name", view.getName());
        recordAuditLog("USER_ROLE_BULK_VIEW_DELETED", "USER_ROLE_BULK_VIEW", view.getId(), details);
    }

    private AdminBulkUserRoleViewDto mapBulkUserRoleView(AdminBulkUserRoleView view) {
        return new AdminBulkUserRoleViewDto(
                view.getId(),
                view.getName(),
                view.getQueryText(),
                readUuidList(view.getUserIdsJson()),
                readStringList(view.getRoleCodesJson()),
                view.getUpdatedAt() == null ? view.getCreatedAt() : view.getUpdatedAt()
        );
    }

    private UUID requireCurrentActorUserId() {
        String actorEmail = currentActorEmail();
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new BadRequestException("ACTOR_REQUIRED", "Authenticated actor context is required");
        }
        return userAccountRepository.findByEmailIgnoreCase(actorEmail)
                .map(UserAccount::getId)
                .orElseThrow(() -> new NotFoundException("ACTOR_USER_NOT_FOUND", "Authenticated actor account not found"));
    }

    private static String normalizeBulkViewName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("BULK_VIEW_NAME_REQUIRED", "Bulk view name is required");
        }
        String normalized = value.trim();
        if (normalized.length() > 120) {
            throw new BadRequestException("BULK_VIEW_NAME_INVALID", "Bulk view name cannot exceed 120 characters");
        }
        return normalized;
    }

    private static String normalizeOptionalQuery(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 255) {
            throw new BadRequestException("BULK_VIEW_QUERY_INVALID", "Bulk view query cannot exceed 255 characters");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static List<UUID> normalizeOptionalUserIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<UUID> readUuidList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            List<UUID> parsed = objectMapper.readValue(value, new TypeReference<List<UUID>>() {
            });
            return parsed == null ? List.of() : parsed.stream().filter(Objects::nonNull).distinct().toList();
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<String> readStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            List<String> parsed = objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
            if (parsed == null) {
                return List.of();
            }
            return parsed.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(code -> !code.isBlank())
                    .map(code -> code.toUpperCase(Locale.ROOT))
                    .distinct()
                    .sorted()
                    .toList();
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String serializeJsonArray(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize bulk view payload", exception);
        }
    }

    private static int countPermissions(Map<String, List<String>> grants) {
        if (grants == null || grants.isEmpty()) {
            return 0;
        }
        return grants.values().stream()
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
    }

    private void replaceRolePermissionsInternal(AdminRole role, Set<AdminPermission> permissions) {
        UUID roleId = role.getId();
        adminRolePermissionRepository.deleteByRoleId(roleId);

        List<AdminRolePermission> assignments = permissions.stream()
                .map(permission -> {
                    AdminRolePermission assignment = new AdminRolePermission();
                    assignment.setId(new AdminRolePermissionId(roleId, permission.getId()));
                    assignment.setRole(role);
                    assignment.setPermission(permission);
                    return assignment;
                })
                .toList();
        adminRolePermissionRepository.saveAll(assignments);
    }

    private void replaceUserRolesInternal(List<UserAccount> users, List<AdminRole> roles) {
        if (users.isEmpty()) {
            return;
        }
        List<UUID> userIds = users.stream().map(UserAccount::getId).toList();
        adminUserRoleRepository.deleteByUserIds(userIds);

        if (roles.isEmpty()) {
            return;
        }
        List<AdminUserRole> assignments = new ArrayList<>();
        for (UserAccount user : users) {
            for (AdminRole role : roles) {
                AdminUserRole assignment = new AdminUserRole();
                assignment.setId(new AdminUserRoleId(user.getId(), role.getId()));
                assignment.setUser(user);
                assignment.setRole(role);
                assignments.add(assignment);
            }
        }
        adminUserRoleRepository.saveAll(assignments);
    }

    private List<AdminRole> resolveAssignableRoles(List<String> roleCodes) {
        Set<String> requestedCodes = normalizeRoleCodes(roleCodes);
        if (requestedCodes.isEmpty()) {
            return List.of();
        }

        List<AdminRole> roles = adminRoleRepository.findAllByCodes(requestedCodes);
        Map<String, AdminRole> byCode = roles.stream()
                .collect(Collectors.toMap(
                        role -> role.getCode().toUpperCase(Locale.ROOT),
                        role -> role,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        if (byCode.size() != requestedCodes.size()) {
            Set<String> missing = new LinkedHashSet<>(requestedCodes);
            missing.removeAll(byCode.keySet());
            throw new BadRequestException("ROLE_NOT_FOUND", "Unknown role codes: " + String.join(", ", missing));
        }

        for (AdminRole role : roles) {
            if (!role.isActive()) {
                throw new BadRequestException("ROLE_INACTIVE", "Cannot assign inactive role: " + role.getCode());
            }
            if (!role.isAssignable()) {
                throw new BadRequestException("ROLE_NOT_ASSIGNABLE", "Role is not assignable: " + role.getCode());
            }
        }
        return byCode.values().stream()
                .sorted((left, right) -> left.getCode().compareToIgnoreCase(right.getCode()))
                .toList();
    }

    private static List<UUID> normalizeUserIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BadRequestException("USER_IDS_REQUIRED", "At least one user id is required");
        }
        List<UUID> normalized = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new BadRequestException("USER_IDS_REQUIRED", "At least one user id is required");
        }
        return normalized;
    }

    private AdminRole requireRole(UUID roleId) {
        return adminRoleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role not found"));
    }

    private UserAccount requireUser(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private List<AdminRolePermissionDto> mapRoles(Collection<AdminRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        List<AdminRole> roleList = new ArrayList<>(roles);
        List<UUID> roleIds = roleList.stream().map(AdminRole::getId).toList();

        Map<UUID, List<AdminRolePermission>> grantsByRoleId = adminRolePermissionRepository.findDetailedByRoleIds(roleIds).stream()
                .collect(Collectors.groupingBy(
                        rolePermission -> rolePermission.getRole().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<UUID, Long> userCountsByRoleId = adminUserRoleRepository.countByRoleIds(roleIds).stream()
                .collect(Collectors.toMap(AdminRoleAssignmentCountProjection::getRoleId, AdminRoleAssignmentCountProjection::getUserCount));

        return roleList.stream()
                .map(role -> {
                    List<AdminRolePermission> rolePermissions = grantsByRoleId.getOrDefault(role.getId(), List.of());
                    Map<String, List<String>> grants = rolePermissions.stream()
                            .collect(Collectors.groupingBy(
                                    rolePermission -> rolePermission.getPermission().getScope(),
                                    LinkedHashMap::new,
                                    Collectors.mapping(
                                            rolePermission -> rolePermission.getPermission().getAction(),
                                            Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new)
                                    )
                            ));

                    Map<String, Boolean> capabilityMap = adminAuthorizationService.capabilitiesForRoles(Set.of(role.getCode()));
                    List<String> capabilities = capabilityMap.entrySet().stream()
                            .filter(Map.Entry::getValue)
                            .map(Map.Entry::getKey)
                            .sorted()
                            .toList();

                    return new AdminRolePermissionDto(
                            role.getId(),
                            role.getCode(),
                            role.getLabel(),
                            role.getDescription(),
                            role.isSystemRole(),
                            role.isAssignable(),
                            role.isActive(),
                            userCountsByRoleId.getOrDefault(role.getId(), 0L),
                            grants,
                            capabilities
                    );
                })
                .toList();
    }

    private AdminRolePermissionDto mapSingleRole(AdminRole role) {
        List<AdminRolePermissionDto> mapped = mapRoles(List.of(role));
        if (mapped.isEmpty()) {
            throw new NotFoundException("ROLE_NOT_FOUND", "Role not found");
        }
        return mapped.getFirst();
    }

    private AdminRbacAuditLogDto toAuditLogDto(AdminRbacAuditLog entry) {
        return new AdminRbacAuditLogDto(
                entry.getId(),
                entry.getActionType(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getActorEmail(),
                entry.getActorUserId(),
                entry.getOutcome(),
                entry.getCorrelationId(),
                entry.getPayloadHash(),
                entry.getDetailsJson(),
                entry.getOccurredAt()
        );
    }

    private void recordAuditLog(String actionType, String entityType, UUID entityId, Object details) {
        AdminRbacAuditLog log = new AdminRbacAuditLog();
        String actorEmail = currentActorEmail();
        log.setActionType(actionType);
        log.setEntityType(entityType);
        log.setEntityId(entityId == null ? null : entityId.toString());
        log.setActorEmail(actorEmail);
        log.setActorUserId(resolveActorUserId(actorEmail));
        log.setOutcome("SUCCESS");
        log.setCorrelationId(MDC.get("correlationId"));
        String detailsJson = serializeDetails(details);
        Instant occurredAt = Instant.now();
        log.setDetailsJson(detailsJson);
        log.setOccurredAt(occurredAt);
        log.setPayloadHash(
                sha256Hex(String.join("|",
                        safe(actionType),
                        safe(entityType),
                        safe(log.getEntityId()),
                        safe(actorEmail),
                        safe(log.getOutcome()),
                        safe(log.getCorrelationId()),
                        safe(detailsJson),
                        String.valueOf(occurredAt.toEpochMilli())
                ))
        );
        adminRbacAuditLogRepository.save(log);
    }

    private UUID resolveActorUserId(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return null;
        }
        return userAccountRepository.findByEmailIgnoreCase(actorEmail)
                .map(UserAccount::getId)
                .orElse(null);
    }

    private String currentActorEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName().trim();
    }

    private String serializeDetails(Object details) {
        if (details == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            return "{\"serializationError\":\"DETAILS_SERIALIZATION_FAILED\"}";
        }
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private Set<AdminPermission> resolvePermissions(Map<String, List<String>> grants) {
        if (grants == null || grants.isEmpty()) {
            return Set.of();
        }
        Map<String, AdminPermission> permissionByKey = adminPermissionRepository.findAllByOrderByDisplayOrderAscScopeAscActionAsc().stream()
                .collect(Collectors.toMap(
                        permission -> permissionKey(permission.getScope(), permission.getAction()),
                        permission -> permission
                ));

        Set<AdminPermission> resolved = new LinkedHashSet<>();
        grants.forEach((scope, actions) -> {
            String normalizedScope = normalizeToken(scope);
            if (actions == null) {
                return;
            }
            for (String action : actions) {
                String normalizedAction = normalizeToken(action);
                if (normalizedScope.isBlank() || normalizedAction.isBlank()) {
                    continue;
                }
                AdminPermission permission = permissionByKey.get(permissionKey(normalizedScope, normalizedAction));
                if (permission == null) {
                    throw new BadRequestException("PERMISSION_NOT_FOUND",
                            "Unknown permission: " + normalizedScope + "." + normalizedAction);
                }
                resolved.add(permission);
            }
        });
        return resolved;
    }

    private static String permissionKey(String scope, String action) {
        return normalizeToken(scope) + "|" + normalizeToken(action);
    }

    private static Set<String> normalizeRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }
        return roleCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new BadRequestException("ROLE_CODE_REQUIRED", "Role code is required");
        }
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeRequiredLabel(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("ROLE_LABEL_REQUIRED", "Role label is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
