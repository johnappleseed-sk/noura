package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.dto.dashboard.DashboardSummaryDto;
import com.noura.platform.dto.user.AdminUserUpdateRequest;
import com.noura.platform.dto.user.ApprovalDto;
import com.noura.platform.dto.user.ApprovalUpdateRequest;
import com.noura.platform.dto.user.UserProfileDto;
import com.noura.platform.service.AdminDashboardService;
import com.noura.platform.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
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
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserProfileDto>> users(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<UserProfileDto> users = userAccountService.listUsers(pageable);
        return ApiResponse.ok("Users", PageResponse.from(users), http.getRequestURI());
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
}
