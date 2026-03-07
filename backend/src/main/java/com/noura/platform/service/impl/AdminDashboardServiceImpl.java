package com.noura.platform.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.ApprovalRequest;
import com.noura.platform.domain.enums.ApprovalStatus;
import com.noura.platform.dto.dashboard.DashboardSummaryDto;
import com.noura.platform.dto.user.ApprovalDto;
import com.noura.platform.dto.user.ApprovalUpdateRequest;
import com.noura.platform.mapper.ApprovalMapper;
import com.noura.platform.repository.*;
import com.noura.platform.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final UserAccountRepository userAccountRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalMapper approvalMapper;

    /**
     * Executes summary.
     *
     * @return The mapped DTO representation.
     */
    @Override
    @Cacheable(cacheNames = "dashboard", key = "'summary'")
    public DashboardSummaryDto summary() {
        BigDecimal revenue = orderRepository.findAll().stream()
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> topProducts = productRepository.findTop10ByBestSellerTrueOrderByPopularityScoreDesc()
                .stream()
                .map(product -> product.getName() + " (" + product.getPopularityScore() + ")")
                .toList();
        List<String> storePerformance = storeRepository.findAll().stream()
                .map(store -> store.getName() + " - " + store.getRegion())
                .toList();
        return new DashboardSummaryDto(
                revenue,
                orderRepository.count(),
                userAccountRepository.count(),
                storeRepository.count(),
                topProducts,
                storePerformance
        );
    }

    /**
     * Executes approval queue.
     *
     * @return A list of matching items.
     */
    @Override
    public List<ApprovalDto> approvalQueue() {
        return approvalRequestRepository.findByStatus(ApprovalStatus.PENDING)
                .stream().map(approvalMapper::toDto).toList();
    }

    /**
     * Updates approval.
     *
     * @param approvalId The approval id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "dashboard", allEntries = true)
    public ApprovalDto updateApproval(UUID approvalId, ApprovalUpdateRequest request) {
        ApprovalRequest approval = approvalRequestRepository.findById(approvalId)
                .orElseThrow(() -> new NotFoundException("APPROVAL_NOT_FOUND", "Approval request not found"));
        approval.setStatus(request.status());
        approval.setReviewerNotes(request.reviewerNotes());
        return approvalMapper.toDto(approvalRequestRepository.save(approval));
    }
}
