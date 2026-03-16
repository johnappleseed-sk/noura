package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.domain.entity.Brand;
import com.noura.platform.dto.brand.BrandResponse;
import com.noura.platform.dto.brand.CreateBrandRequest;
import com.noura.platform.repository.BrandRepository;
import com.noura.platform.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PRODUCT_MANAGER')")
    public BrandResponse createBrand(CreateBrandRequest request) {
        String code = normalizeCode(request.code());
        String slug = normalizeSlug(request.slug());
        if (brandRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("BRAND_CODE_DUPLICATE", "Brand code already exists");
        }
        if (brandRepository.existsBySlugIgnoreCase(slug)) {
            throw new BadRequestException("BRAND_SLUG_DUPLICATE", "Brand slug already exists");
        }
        brandRepository.findByNameIgnoreCase(request.name().trim()).ifPresent(existing -> {
            throw new BadRequestException("BRAND_NAME_DUPLICATE", "Brand name already exists");
        });

        Brand brand = new Brand();
        brand.setCode(code);
        brand.setName(request.name().trim());
        brand.setSlug(slug);
        brand.setActive(request.active() == null || request.active());
        return toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PRODUCT_MANAGER')")
    public List<BrandResponse> listBrands() {
        return brandRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getCode(),
                brand.getName(),
                brand.getSlug(),
                brand.isActive(),
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("BRAND_CODE_REQUIRED", "Brand code is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSlug(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("BRAND_SLUG_REQUIRED", "Brand slug is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new BadRequestException("BRAND_SLUG_REQUIRED", "Brand slug is required");
        }
        return normalized;
    }
}
