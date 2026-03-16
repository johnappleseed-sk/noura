package com.noura.platform.audit;

import com.noura.platform.domain.entity.Merchant;
import com.noura.platform.domain.entity.ProductSubmission;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.dto.auth.AuthTokensResponse;
import com.noura.platform.dto.auth.RegisterRequest;
import com.noura.platform.dto.merchant.MerchantResponse;
import com.noura.platform.dto.store.StoreResponse;
import com.noura.platform.dto.superinventory.ProductSubmissionResponse;
import com.noura.platform.repository.MerchantRepository;
import com.noura.platform.repository.ProductSubmissionRepository;
import com.noura.platform.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminGovernanceAuditAspect {

    private final AuditLoggingHelper auditLoggingHelper;
    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final ProductSubmissionRepository productSubmissionRepository;

    @Around("execution(* com.noura.platform.service.impl.MerchantServiceImpl.updateMerchantStatus(..)) && args(merchantId, ..)")
    public Object auditMerchantStatusChange(ProceedingJoinPoint joinPoint, UUID merchantId) throws Throwable {
        Merchant before = merchantRepository.findById(merchantId).orElse(null);
        Object result = joinPoint.proceed();
        if (result instanceof MerchantResponse response) {
            Map<String, Object> oldValue = null;
            if (before != null) {
                oldValue = new LinkedHashMap<>();
                oldValue.put("status", before.getStatus());
                oldValue.put("displayName", before.getDisplayName());
                oldValue.put("merchantCode", before.getMerchantCode());
            }
            Map<String, Object> newValue = new LinkedHashMap<>();
            newValue.put("status", response.status());
            newValue.put("displayName", response.displayName());
            newValue.put("merchantCode", response.merchantCode());
            auditLoggingHelper.logAction(
                    "MERCHANT_STATUS_UPDATED",
                    "MERCHANT",
                    merchantId,
                    oldValue,
                    newValue
            );
        }
        return result;
    }

    @Around("execution(* com.noura.platform.service.impl.StoreServiceImpl.updateStoreStatus(..)) && args(storeId, ..)")
    public Object auditStoreStatusChange(ProceedingJoinPoint joinPoint, UUID storeId) throws Throwable {
        Store before = storeRepository.findById(storeId).orElse(null);
        Object result = joinPoint.proceed();
        if (result instanceof StoreResponse response) {
            Map<String, Object> oldValue = null;
            if (before != null) {
                oldValue = new LinkedHashMap<>();
                oldValue.put("status", before.getStatus());
                oldValue.put("active", before.isActive());
                oldValue.put("storeCode", before.getStoreCode());
            }
            Map<String, Object> newValue = new LinkedHashMap<>();
            newValue.put("status", response.status());
            newValue.put("active", response.status() != null && "ACTIVE".equalsIgnoreCase(response.status().name()));
            newValue.put("storeCode", response.storeCode());
            auditLoggingHelper.logAction(
                    "STORE_STATUS_UPDATED",
                    "STORE",
                    storeId,
                    oldValue,
                    newValue
            );
        }
        return result;
    }

    @Around("execution(* com.noura.platform.service.impl.SuperInventoryServiceImpl.approveProductSubmission(..)) && args(submissionId, ..)")
    public Object auditProductApproval(ProceedingJoinPoint joinPoint, UUID submissionId) throws Throwable {
        ProductSubmission before = productSubmissionRepository.findById(submissionId).orElse(null);
        Object result = joinPoint.proceed();
        if (result instanceof ProductSubmissionResponse response) {
            Map<String, Object> oldValue = null;
            if (before != null) {
                oldValue = new LinkedHashMap<>();
                oldValue.put("status", before.getStatus());
                oldValue.put("reviewedBy", before.getReviewedBy());
                oldValue.put("reviewNotes", before.getReviewNotes());
            }
            Map<String, Object> newValue = new LinkedHashMap<>();
            newValue.put("status", response.status());
            newValue.put("reviewedBy", response.reviewedBy());
            newValue.put("reviewNotes", response.reviewNotes());
            newValue.put("targetProductId", response.targetProductId());
            auditLoggingHelper.logAction(
                    "PRODUCT_SUBMISSION_APPROVED",
                    "PRODUCT_SUBMISSION",
                    submissionId,
                    oldValue,
                    newValue
            );
        }
        return result;
    }

    @Around("execution(* com.noura.platform.service.impl.SuperInventoryServiceImpl.rejectProductSubmission(..)) && args(submissionId, ..)")
    public Object auditProductRejection(ProceedingJoinPoint joinPoint, UUID submissionId) throws Throwable {
        ProductSubmission before = productSubmissionRepository.findById(submissionId).orElse(null);
        Object result = joinPoint.proceed();
        if (result instanceof ProductSubmissionResponse response) {
            Map<String, Object> oldValue = null;
            if (before != null) {
                oldValue = new LinkedHashMap<>();
                oldValue.put("status", before.getStatus());
                oldValue.put("reviewedBy", before.getReviewedBy());
                oldValue.put("reviewNotes", before.getReviewNotes());
            }
            Map<String, Object> newValue = new LinkedHashMap<>();
            newValue.put("status", response.status());
            newValue.put("reviewedBy", response.reviewedBy());
            newValue.put("reviewNotes", response.reviewNotes());
            auditLoggingHelper.logAction(
                    "PRODUCT_SUBMISSION_REJECTED",
                    "PRODUCT_SUBMISSION",
                    submissionId,
                    oldValue,
                    newValue
            );
        }
        return result;
    }

    @Around("execution(* com.noura.platform.service.impl.AuthServiceImpl.register(..)) && args(request)")
    public Object auditUserCreation(ProceedingJoinPoint joinPoint, RegisterRequest request) throws Throwable {
        Object result = joinPoint.proceed();
        if (result instanceof AuthTokensResponse response) {
            Map<String, Object> newValue = new LinkedHashMap<>();
            newValue.put("userId", response.userId());
            newValue.put("email", response.email());
            newValue.put("fullName", response.fullName());
            newValue.put("roles", response.roles());
            newValue.put("registrationEmail", request.email());
            auditLoggingHelper.logAction(
                    "USER_CREATED",
                    "USER",
                    response.userId(),
                    null,
                    newValue
            );
        }
        return result;
    }
}
