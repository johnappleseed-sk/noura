package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.dto.dashboard.DashboardSummaryDto;
import com.noura.platform.dto.admin.AdminAuthorizationMatrixDto;
import com.noura.platform.dto.admin.AdminCapabilitiesDto;
import com.noura.platform.dto.user.AdminUserUpdateRequest;
import com.noura.platform.dto.user.ApprovalDto;
import com.noura.platform.dto.user.ApprovalUpdateRequest;
import com.noura.platform.dto.user.UserProfileDto;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.service.AdminDashboardService;
import com.noura.platform.service.AdminAuthorizationService;
import com.noura.platform.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Exposes admin dashboard, user-management, and capability-discovery endpoints.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final UserAccountService userAccountService;

    /**
     * Executes summary.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/dashboard/summary")
    public ApiResponse<DashboardSummaryDto> summary(HttpServletRequest http) {
        return ApiResponse.ok("Dashboard summary", adminDashboardService.summary(), http.getRequestURI());
    }

    /**
     * Executes approval queue.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/b2b/approvals")
    public ApiResponse<List<ApprovalDto>> approvalQueue(HttpServletRequest http) {
        return ApiResponse.ok("B2B approval queue", adminDashboardService.approvalQueue(), http.getRequestURI());
    }

    /**
     * Updates approval.
     *
     * @param approvalId The approval id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PatchMapping("/b2b/approvals/{approvalId}")
    public ApiResponse<ApprovalDto> updateApproval(
            @PathVariable UUID approvalId,
            @Valid @RequestBody ApprovalUpdateRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Approval updated", adminDashboardService.updateApproval(approvalId, request), http.getRequestURI());
    }

    /**
     * Executes users.
     *
     * @param page The pagination configuration.
     * @param size The size value.
     * @param sortBy The sort by value.
     * @param direction The direction value.
     * @param query Optional free-text user search query.
     * @param enabled Optional enabled-status filter.
     * @param role Optional role membership filter.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserProfileDto>> users(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) RoleType role,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<UserProfileDto> users = userAccountService.listUsers(query, enabled, role, pageable);
        return ApiResponse.ok("Users", PageResponse.from(users), http.getRequestURI());
    }

    /**
     * Resolves capability flags for the current admin session.
     *
     * @param authentication The current authentication.
     * @param http The current HTTP request.
     * @return The capability response payload.
     */
    @GetMapping("/capabilities")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AdminCapabilitiesDto> capabilities(Authentication authentication, HttpServletRequest http) {
        Set<String> roles = resolveRoles(authentication);
        Map<String, Boolean> capabilities = adminAuthorizationService.capabilitiesForRoles(roles);

        return ApiResponse.ok(
                "Admin capabilities",
                new AdminCapabilitiesDto(roles.stream().sorted().toList(), capabilities),
                http.getRequestURI()
        );
    }

    /**
     * Returns a versioned role-permission matrix used by the admin permissions UI.
     *
     * @param http The current HTTP request.
     * @return The matrix payload in the standard response envelope.
     */
    @GetMapping("/authorization/matrix")
    @PreAuthorize("hasAuthority('PERM_ROLES_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<AdminAuthorizationMatrixDto> authorizationMatrix(HttpServletRequest http) {
        return ApiResponse.ok(
                "Admin authorization matrix",
                adminAuthorizationService.matrix(),
                http.getRequestURI()
        );
    }

    /**
     * Updates user.
     *
     * @param userId The user id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PatchMapping("/users/{userId}")
    public ApiResponse<UserProfileDto> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserUpdateRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("User updated", userAccountService.adminUpdateUser(userId, request), http.getRequestURI());
    }

    /**
     * Resolves normalized portal roles from the current authentication.
     *
     * @param authentication The current authentication.
     * @return The normalized admin role set.
     */
    private Set<String> resolveRoles(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(value -> value.startsWith("ROLE_") ? value.substring(5) : value)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .filter(adminAuthorizationService.knownRoleCodes()::contains)
                .collect(Collectors.toSet());
    }
}
