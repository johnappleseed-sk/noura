package com.noura.platform.repository;

import com.noura.platform.domain.entity.ApprovalRequest;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
    /**
     * Finds by requester.
     *
     * @param requester The request payload for this operation.
     * @return A list of matching items.
     */
    List<ApprovalRequest> findByRequester(UserAccount requester);

    /**
     * Finds by status.
     *
     * @param status The status value.
     * @return A list of matching items.
     */
    List<ApprovalRequest> findByStatus(ApprovalStatus status);
}
