package com.noura.platform.service;

import com.noura.platform.domain.enums.MerchantContractStatus;
import com.noura.platform.domain.enums.MerchantStatus;
import com.noura.platform.dto.contract.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Contract-based merchant onboarding and store tenant registration.
 */
public interface MerchantContractService {
    MerchantDto createMerchant(MerchantCreateRequest request);

    Page<MerchantDto> listMerchants(String query, MerchantStatus status, Pageable pageable);

    MerchantDto getMerchant(UUID merchantId);

    MerchantContractDto createContract(UUID merchantId, MerchantContractCreateRequest request);

    Page<MerchantContractDto> listContracts(String query, MerchantContractStatus status, UUID merchantId, Pageable pageable);

    MerchantContractDto getContract(UUID contractId);

    List<MerchantContractActionDto> contractActions(UUID contractId);

    MerchantContractDto approveContract(UUID contractId, MerchantContractDecisionRequest request);

    MerchantContractDto rejectContract(UUID contractId, MerchantContractDecisionRequest request);

    MerchantContractDto suspendContract(UUID contractId, MerchantContractDecisionRequest request);

    MerchantContractDto terminateContract(UUID contractId, MerchantContractDecisionRequest request);

    StoreTenantDto registerStore(UUID contractId, ContractStoreRegistrationRequest request);

    StoreStaffAssignmentDto upsertStoreAssignment(UUID storeId, StoreStaffAssignmentRequest request);

    List<StoreStaffAssignmentDto> listStoreAssignments(UUID storeId);
}

