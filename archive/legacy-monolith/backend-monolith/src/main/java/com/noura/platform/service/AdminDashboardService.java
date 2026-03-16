package com.noura.platform.service;

import com.noura.platform.dto.dashboard.DashboardSummaryDto;
import com.noura.platform.dto.user.ApprovalDto;
import com.noura.platform.dto.user.ApprovalUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface AdminDashboardService {
    /**
     * Executes summary.
     *
     * @return The mapped DTO representation.
     */
    DashboardSummaryDto summary();

    /**
     * Executes approval queue.
     *
     * @return A list of matching items.
     */
    List<ApprovalDto> approvalQueue();

    /**
     * Updates approval.
     *
     * @param approvalId The approval id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    ApprovalDto updateApproval(UUID approvalId, ApprovalUpdateRequest request);
}
