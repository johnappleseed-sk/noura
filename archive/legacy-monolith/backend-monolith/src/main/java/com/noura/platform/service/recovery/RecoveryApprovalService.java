package com.noura.platform.service.recovery;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryApprovalStatus;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
import com.noura.platform.dto.recovery.RecoveryApprovalRequestDto;
import com.noura.platform.dto.recovery.RecoveryBulkActionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Defines approval workflows for high-impact recovery actions.
 */
public interface RecoveryApprovalService {

    Page<RecoveryApprovalRequestDto> listApprovalRequests(
            String entityType,
            RecoveryActionType actionType,
            RecoveryApprovalStatus status,
            String query,
            Pageable pageable
    );

    RecoveryApprovalRequestDto requestActionApproval(RecoveryActionRequest request, String actor);

    RecoveryApprovalRequestDto requestBulkApproval(RecoveryBulkActionRequest request, String actor);

    RecoveryApprovalRequestDto approve(UUID approvalId, String reviewer, String reviewerNotes);

    RecoveryApprovalRequestDto reject(UUID approvalId, String reviewer, String reviewerNotes);
}

