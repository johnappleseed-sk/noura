package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Merchant;
import com.noura.platform.domain.enums.MerchantStatus;
import com.noura.platform.dto.merchant.CreateMerchantRequest;
import com.noura.platform.dto.merchant.MerchantResponse;
import com.noura.platform.dto.merchant.UpdateMerchantStatusRequest;
import com.noura.platform.repository.MerchantRepository;
import com.noura.platform.service.MerchantService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_MERCHANTS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Page<MerchantResponse> listMerchants(String search, MerchantStatus status, Pageable pageable) {
        Specification<Merchant> spec = buildMerchantSpecification(search, status);
        return merchantRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_MERCHANTS_CREATE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantResponse createMerchant(CreateMerchantRequest request) {
        Merchant merchant = new Merchant();
        merchant.setMerchantCode(normalizeUpperTrim(request.merchantCode()));
        if (merchantRepository.existsByMerchantCodeIgnoreCase(merchant.getMerchantCode())) {
            throw new BadRequestException("MERCHANT_CODE_DUPLICATE", "Merchant code already exists");
        }
        merchant.setDisplayName(trim(request.displayName()));
        merchant.setName(trim(request.displayName()));
        merchant.setLegalName(trim(request.legalName()));
        merchant.setEmail(trim(request.email()));
        merchant.setPhone(trim(request.phone()));
        merchant.setCountryCode(normalizeCountryCode(request.countryCode()));
        merchant.setContractStartAt(request.contractStartAt());
        merchant.setContractEndAt(request.contractEndAt());
        validateDateWindow(request.contractStartAt(), request.contractEndAt());
        merchant.setNotes(trim(request.notes()));
        merchant.setStatus(request.status() == null ? MerchantStatus.ACTIVE : request.status());
        Merchant saved = merchantRepository.save(merchant);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_MERCHANTS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantResponse getMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("MERCHANT_NOT_FOUND", "Merchant not found"));
        return toResponse(merchant);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_MERCHANTS_UPDATE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public MerchantResponse updateMerchantStatus(UUID merchantId, UpdateMerchantStatusRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("MERCHANT_NOT_FOUND", "Merchant not found"));
        merchant.setStatus(request.status());
        return toResponse(merchantRepository.save(merchant));
    }

    private Specification<Merchant> buildMerchantSpecification(String search, MerchantStatus status) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("legalName")), like),
                        cb.like(cb.lower(root.get("displayName")), like)
                ));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private MerchantResponse toResponse(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getMerchantCode(),
                merchant.getLegalName(),
                merchant.getDisplayName(),
                merchant.getEmail(),
                merchant.getPhone(),
                merchant.getCountryCode(),
                merchant.getStatus(),
                merchant.getContractStartAt(),
                merchant.getContractEndAt(),
                merchant.getNotes(),
                merchant.getCreatedAt(),
                merchant.getUpdatedAt(),
                merchant.getCreatedBy(),
                merchant.getUpdatedBy()
        );
    }

    private String normalizeUpperTrim(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private void validateDateWindow(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("MERCHANT_CONTRACT_DATES_INVALID", "Contract end date must be on or after start date");
        }
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
