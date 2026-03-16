package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Merchant;
import com.noura.platform.domain.entity.MerchantContract;
import com.noura.platform.domain.entity.MerchantContractAction;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.StoreTenant;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.entity.UserStoreAssignment;
import com.noura.platform.domain.enums.MerchantContractActionType;
import com.noura.platform.domain.enums.MerchantContractStatus;
import com.noura.platform.domain.enums.MerchantStatus;
import com.noura.platform.domain.enums.StoreTenantStatus;
import com.noura.platform.dto.contract.*;
import com.noura.platform.repository.MerchantContractActionRepository;
import com.noura.platform.repository.MerchantContractRepository;
import com.noura.platform.repository.MerchantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.UserStoreAssignmentRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.MerchantContractService;
import com.noura.platform.service.StoreService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantContractServiceImpl implements MerchantContractService {

    private final MerchantRepository merchantRepository;
    private final MerchantContractRepository contractRepository;
    private final MerchantContractActionRepository contractActionRepository;
    private final StoreTenantRepository storeTenantRepository;
    private final StoreRepository storeRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserStoreAssignmentRepository userStoreAssignmentRepository;
    private final StoreService storeService;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_CREATE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantDto createMerchant(MerchantCreateRequest request) {
        String name = normalizeRequired(request.name(), "MERCHANT_NAME_REQUIRED", "Merchant name is required.");
        String merchantCode = generateFallbackMerchantCode(name);
        if (merchantRepository.existsByMerchantCodeIgnoreCase(merchantCode)) {
            merchantCode = normalizeCode(merchantCode) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
            if (merchantRepository.existsByMerchantCodeIgnoreCase(merchantCode)) {
                throw new BadRequestException("MERCHANT_CODE_EXISTS", "Generated merchant code already exists. Please retry.");
            }
        }
        merchantRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw new BadRequestException("MERCHANT_EXISTS", "Merchant name already exists");
        });
        Merchant merchant = new Merchant();
        merchant.setName(name);
        merchant.setDisplayName(name);
        merchant.setMerchantCode(merchantCode);
        merchant.setLegalName(trimToNull(request.legalName()));
        merchant.setTaxId(trimToNull(request.taxId()));
        merchant.setPrimaryEmail(trimToNull(request.primaryEmail()));
        merchant.setPrimaryPhone(trimToNull(request.primaryPhone()));
        merchant.setNotes(trimToNull(request.notes()));
        merchant.setStatus(MerchantStatus.ACTIVE);
        Merchant saved = merchantRepository.save(merchant);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Page<MerchantDto> listMerchants(String query, MerchantStatus status, Pageable pageable) {
        Specification<Merchant> spec = buildMerchantSpec(query, status);
        return merchantRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantDto getMerchant(UUID merchantId) {
        return toDto(requireMerchant(merchantId));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_CREATE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantContractDto createContract(UUID merchantId, MerchantContractCreateRequest request) {
        Merchant merchant = requireMerchant(merchantId);
        String contractNumber = normalizeRequired(request.contractNumber(), "CONTRACT_NUMBER_REQUIRED", "Contract number is required.");
        contractRepository.findByContractNumberIgnoreCase(contractNumber).ifPresent(existing -> {
            throw new BadRequestException("CONTRACT_EXISTS", "Contract number already exists");
        });
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("CONTRACT_DATES_INVALID", "Contract endDate must be after startDate");
        }

        MerchantContract contract = new MerchantContract();
        contract.setMerchant(merchant);
        contract.setContractNumber(contractNumber);
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setRequestedBy(currentUser());
        contract.setStatus(MerchantContractStatus.PENDING_APPROVAL);
        contract.setReviewNote(trimToNull(request.note()));
        contract.setTerms(request.terms() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.terms()));
        MerchantContract saved = contractRepository.save(contract);
        writeContractAction(saved, MerchantContractActionType.CREATED, request.note(), Map.of());
        writeContractAction(saved, MerchantContractActionType.SUBMITTED, request.note(), Map.of());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Page<MerchantContractDto> listContracts(String query, MerchantContractStatus status, UUID merchantId, Pageable pageable) {
        Specification<MerchantContract> spec = buildContractSpec(query, status, merchantId);
        return contractRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantContractDto getContract(UUID contractId) {
        return toDto(requireContract(contractId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<MerchantContractActionDto> contractActions(UUID contractId) {
        requireContract(contractId);
        return contractActionRepository.findByContractIdOrderByOccurredAtDesc(contractId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantContractDto approveContract(UUID contractId, MerchantContractDecisionRequest request) {
        MerchantContract contract = requireContract(contractId);
        if (contract.getStatus() == MerchantContractStatus.TERMINATED) {
            throw new BadRequestException("CONTRACT_TERMINATED", "Contract is terminated");
        }
        contract.setStatus(MerchantContractStatus.APPROVED);
        contract.setReviewedBy(currentUser());
        contract.setReviewedAt(Instant.now());
        contract.setReviewNote(trimToNull(request.note()));
        MerchantContract saved = contractRepository.save(contract);
        writeContractAction(saved, MerchantContractActionType.APPROVED, request.note(), Map.of());
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantContractDto rejectContract(UUID contractId, MerchantContractDecisionRequest request) {
        MerchantContract contract = requireContract(contractId);
        if (contract.getStatus() == MerchantContractStatus.APPROVED) {
            throw new BadRequestException("CONTRACT_ALREADY_APPROVED", "Approved contracts cannot be rejected; suspend or terminate instead.");
        }
        contract.setStatus(MerchantContractStatus.REJECTED);
        contract.setReviewedBy(currentUser());
        contract.setReviewedAt(Instant.now());
        contract.setReviewNote(trimToNull(request.note()));
        MerchantContract saved = contractRepository.save(contract);
        writeContractAction(saved, MerchantContractActionType.REJECTED, request.note(), Map.of());
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_UPDATE') or hasAuthority('PERM_CONTRACTS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantContractDto suspendContract(UUID contractId, MerchantContractDecisionRequest request) {
        MerchantContract contract = requireContract(contractId);
        if (contract.getStatus() != MerchantContractStatus.APPROVED) {
            throw new BadRequestException("CONTRACT_NOT_APPROVED", "Only approved contracts can be suspended");
        }
        contract.setStatus(MerchantContractStatus.SUSPENDED);
        contract.setReviewedBy(currentUser());
        contract.setReviewedAt(Instant.now());
        contract.setReviewNote(trimToNull(request.note()));
        MerchantContract saved = contractRepository.save(contract);
        writeContractAction(saved, MerchantContractActionType.SUSPENDED, request.note(), Map.of());
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantContractDto terminateContract(UUID contractId, MerchantContractDecisionRequest request) {
        MerchantContract contract = requireContract(contractId);
        contract.setStatus(MerchantContractStatus.TERMINATED);
        contract.setReviewedBy(currentUser());
        contract.setReviewedAt(Instant.now());
        contract.setReviewNote(trimToNull(request.note()));
        MerchantContract saved = contractRepository.save(contract);
        writeContractAction(saved, MerchantContractActionType.TERMINATED, request.note(), Map.of());

        // Defensive: terminate any linked store tenant records.
        storeTenantRepository.findByContractId(contractId).forEach(tenant -> {
            tenant.setStatus(StoreTenantStatus.TERMINATED);
            tenant.setDeactivatedAt(Instant.now());
            storeTenantRepository.save(tenant);
            Store store = tenant.getStore();
            if (store != null && store.isActive()) {
                store.setActive(false);
                storeRepository.save(store);
            }
        });

        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_UPDATE') or hasAuthority('PERM_CONTRACTS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public StoreTenantDto registerStore(UUID contractId, ContractStoreRegistrationRequest request) {
        MerchantContract contract = requireContract(contractId);
        ensureContractValidForActivation(contract);

        var createdStore = storeService.createStore(request.store());
        Store store = storeRepository.findById(createdStore.id())
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        UUID merchantId = contract.getMerchant().getId();
        if (store.getMerchantId() == null) {
            store.setMerchantId(merchantId);
            storeRepository.save(store);
        } else if (!store.getMerchantId().equals(merchantId)) {
            throw new BadRequestException("STORE_MERCHANT_MISMATCH", "Store is linked to a different merchant");
        }

        StoreTenant tenant = storeTenantRepository.findByStoreId(store.getId()).orElseGet(StoreTenant::new);
        if (tenant.getId() != null) {
            throw new BadRequestException("STORE_ALREADY_REGISTERED", "Store already has a tenant registration");
        }
        tenant.setStore(store);
        tenant.setMerchant(contract.getMerchant());
        tenant.setContract(contract);
        tenant.setStatus(StoreTenantStatus.ACTIVE);
        tenant.setActivatedAt(Instant.now());
        StoreTenant savedTenant = storeTenantRepository.save(tenant);

        if (request.primaryAdminUserId() != null) {
            upsertStoreAssignment(store.getId(), new StoreStaffAssignmentRequest(request.primaryAdminUserId(), "STORE_ADMIN", true));
        }

        writeContractAction(contract, MerchantContractActionType.STORE_REGISTERED, null, Map.of(
                "storeId", store.getId().toString(),
                "storeName", store.getName()
        ));

        return toDto(savedTenant);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_UPDATE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public StoreStaffAssignmentDto upsertStoreAssignment(UUID storeId, StoreStaffAssignmentRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        UserAccount user = userAccountRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        UserStoreAssignment assignment = userStoreAssignmentRepository.findByUserIdAndStoreId(user.getId(), store.getId())
                .orElseGet(UserStoreAssignment::new);
        assignment.setUser(user);
        assignment.setStore(store);
        assignment.setRoleCode(trimToNull(request.roleCode()));
        if (request.active() != null) {
            assignment.setActive(request.active());
        }
        UserStoreAssignment saved = userStoreAssignmentRepository.save(assignment);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_CONTRACTS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<StoreStaffAssignmentDto> listStoreAssignments(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        return userStoreAssignmentRepository.findByStoreIdAndActiveTrue(storeId).stream()
                .map(assignment -> toDto(assignment, store))
                .toList();
    }

    private Specification<Merchant> buildMerchantSpec(String query, MerchantStatus status) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("legalName")), like),
                        cb.like(cb.lower(root.get("displayName")), like),
                        cb.like(cb.lower(root.get("primaryEmail")), like)
                ));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<MerchantContract> buildContractSpec(String query, MerchantContractStatus status, UUID merchantId) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (merchantId != null) {
                predicates.add(cb.equal(root.get("merchant").get("id"), merchantId));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("contractNumber")), like),
                        cb.like(cb.lower(root.get("merchant").get("name")), like)
                ));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Merchant requireMerchant(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("MERCHANT_NOT_FOUND", "Merchant not found"));
    }

    private MerchantContract requireContract(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("CONTRACT_NOT_FOUND", "Contract not found"));
    }

    private void ensureContractValidForActivation(MerchantContract contract) {
        if (contract.getStatus() != MerchantContractStatus.APPROVED) {
            throw new BadRequestException("CONTRACT_NOT_APPROVED", "Contract must be APPROVED to register/activate a store");
        }
        LocalDate today = LocalDate.now();
        if (contract.getStartDate() != null && today.isBefore(contract.getStartDate())) {
            throw new BadRequestException("CONTRACT_NOT_STARTED", "Contract has not started yet");
        }
        if (contract.getEndDate() != null && today.isAfter(contract.getEndDate())) {
            throw new BadRequestException("CONTRACT_EXPIRED", "Contract has expired");
        }
    }

    private void writeContractAction(MerchantContract contract, MerchantContractActionType action, String note, Map<String, Object> metadata) {
        MerchantContractAction entry = new MerchantContractAction();
        entry.setContract(contract);
        entry.setAction(action);
        entry.setActorEmail(SecurityUtils.currentEmail());
        entry.setActorUser(currentUserOptional());
        entry.setNote(trimToNull(note));
        entry.setCorrelationId(MDC.get("correlationId"));
        entry.setMetadata(metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata));
        entry.setOccurredAt(Instant.now());
        contractActionRepository.save(entry);
    }

    private UserAccount currentUser() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private UserAccount currentUserOptional() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail()).orElse(null);
    }

    private MerchantDto toDto(Merchant merchant) {
        return new MerchantDto(
                merchant.getId(),
                merchant.getName(),
                merchant.getLegalName(),
                merchant.getTaxId(),
                merchant.getPrimaryEmail(),
                merchant.getPrimaryPhone(),
                merchant.getStatus(),
                merchant.getNotes(),
                merchant.getCreatedAt()
        );
    }

    private MerchantContractDto toDto(MerchantContract contract) {
        return new MerchantContractDto(
                contract.getId(),
                contract.getMerchant().getId(),
                contract.getMerchant().getName(),
                contract.getContractNumber(),
                contract.getStatus(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getRequestedBy() == null ? null : contract.getRequestedBy().getEmail(),
                contract.getReviewedBy() == null ? null : contract.getReviewedBy().getEmail(),
                contract.getReviewedAt(),
                contract.getReviewNote(),
                contract.getTerms(),
                contract.getCreatedAt()
        );
    }

    private MerchantContractActionDto toDto(MerchantContractAction action) {
        return new MerchantContractActionDto(
                action.getId(),
                action.getContract().getId(),
                action.getAction(),
                action.getActorEmail(),
                action.getNote(),
                action.getCorrelationId(),
                action.getMetadata(),
                action.getOccurredAt()
        );
    }

    private StoreTenantDto toDto(StoreTenant tenant) {
        return new StoreTenantDto(
                tenant.getId(),
                tenant.getStore().getId(),
                tenant.getStore().getName(),
                tenant.getMerchant().getId(),
                tenant.getMerchant().getName(),
                tenant.getContract().getId(),
                tenant.getContract().getContractNumber(),
                tenant.getStatus(),
                tenant.getActivatedAt(),
                tenant.getDeactivatedAt(),
                tenant.getCreatedAt()
        );
    }

    private StoreStaffAssignmentDto toDto(UserStoreAssignment assignment) {
        Store store = assignment.getStore();
        return toDto(assignment, store);
    }

    private StoreStaffAssignmentDto toDto(UserStoreAssignment assignment, Store store) {
        return new StoreStaffAssignmentDto(
                assignment.getId(),
                assignment.getUser().getId(),
                assignment.getUser().getEmail(),
                store.getId(),
                store.getName(),
                assignment.getRoleCode(),
                assignment.isActive(),
                assignment.getCreatedAt()
        );
    }

    private String normalizeRequired(String value, String code, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new BadRequestException(code, message);
        }
        return normalized;
    }

    private String generateFallbackMerchantCode(String merchantName) {
        String prefix = merchantName == null || merchantName.isBlank()
                ? "MER"
                : merchantName.trim().replaceAll("[^A-Za-z0-9]", "_").toUpperCase(Locale.ROOT);
        if (prefix.length() > 30) {
            prefix = prefix.substring(0, 30);
        }
        return ("MER-" + prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6)).toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
