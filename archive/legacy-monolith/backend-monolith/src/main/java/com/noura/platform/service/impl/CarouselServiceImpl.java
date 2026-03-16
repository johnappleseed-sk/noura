package com.noura.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.CarouselSlide;
import com.noura.platform.domain.enums.CarouselBulkActionType;
import com.noura.platform.domain.enums.CarouselLinkType;
import com.noura.platform.domain.enums.CarouselStatus;
import com.noura.platform.domain.enums.CarouselVisibility;
import com.noura.platform.dto.carousel.CarouselBulkActionRequest;
import com.noura.platform.dto.carousel.CarouselPreviewDto;
import com.noura.platform.dto.carousel.CarouselReorderItemRequest;
import com.noura.platform.dto.carousel.CarouselSlideDto;
import com.noura.platform.dto.carousel.CarouselSlideRequest;
import com.noura.platform.dto.carousel.StorefrontCarouselSlideDto;
import com.noura.platform.repository.CarouselSlideRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.CarouselService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CarouselServiceImpl implements CarouselService {
    private static final Pattern IMAGE_REF_PATTERN = Pattern.compile("(?i).+\\.(png|jpe?g|webp|gif|svg|avif)(\\?.*)?$");

    private final CarouselSlideRepository carouselSlideRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CarouselSlideDto> listAdminCarousels(String q,
                                                     CarouselStatus status,
                                                     Boolean published,
                                                     UUID storeId,
                                                     String locale,
                                                     Boolean includeDeleted,
                                                     Instant startFrom,
                                                     Instant startTo,
                                                     Pageable pageable) {
        Specification<CarouselSlide> spec = Specification.where(null);
        if (!Boolean.TRUE.equals(includeDeleted)) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("deletedAt")));
        }
        if (hasText(q)) {
            String term = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), term),
                    cb.like(cb.lower(root.get("slug")), term),
                    cb.like(cb.lower(root.get("description")), term)
            ));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (published != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("published"), published));
        }
        if (storeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("storeId"), storeId));
        }
        if (hasText(locale)) {
            String normalizedLocale = locale.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("locale")), normalizedLocale));
        }
        if (startFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), startFrom));
        }
        if (startTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("startAt"), startTo));
        }
        return carouselSlideRepository.findAll(spec, pageable).map(this::toAdminDto);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CarouselSlideDto getAdminCarousel(UUID carouselId, boolean includeDeleted) {
        return toAdminDto(requireCarousel(carouselId, includeDeleted));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "carouselSlides", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CarouselSlideDto createCarousel(CarouselSlideRequest request) {
        CarouselSlide entity = new CarouselSlide();
        applyRequest(entity, request, true);
        entity.setCreatedBy(currentActor());
        entity.setUpdatedBy(currentActor());
        return toAdminDto(carouselSlideRepository.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "carouselSlides", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CarouselSlideDto updateCarousel(UUID carouselId, CarouselSlideRequest request) {
        CarouselSlide entity = requireCarousel(carouselId, true);
        applyRequest(entity, request, false);
        entity.setUpdatedBy(currentActor());
        entity.setVersionNumber(nextVersion(entity.getVersionNumber()));
        return toAdminDto(carouselSlideRepository.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "carouselSlides", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCarousel(UUID carouselId) {
        CarouselSlide entity = requireCarousel(carouselId, false);
        entity.setDeletedAt(Instant.now());
        entity.setDeletedBy(currentActor());
        entity.setUpdatedBy(currentActor());
        entity.setPublished(false);
        entity.setStatus(CarouselStatus.ARCHIVED);
        entity.setVersionNumber(nextVersion(entity.getVersionNumber()));
        carouselSlideRepository.save(entity);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "carouselSlides", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CarouselSlideDto restoreCarousel(UUID carouselId) {
        CarouselSlide entity = requireCarousel(carouselId, true);
        if (entity.getDeletedAt() == null) {
            return toAdminDto(entity);
        }
        entity.setDeletedAt(null);
        entity.setDeletedBy(null);
        entity.setPublished(false);
        entity.setStatus(CarouselStatus.DRAFT);
        entity.setUpdatedBy(currentActor());
        entity.setVersionNumber(nextVersion(entity.getVersionNumber()));
        return toAdminDto(carouselSlideRepository.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "carouselSlides", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CarouselSlideDto changeStatus(UUID carouselId, CarouselStatus status) {
        CarouselSlide entity = requireCarousel(carouselId, false);
        if (status == CarouselStatus.SCHEDULED && entity.getStartAt() == null) {
            throw new BadRequestException("CAROUSEL_SCHEDULE_REQUIRED", "Scheduled slides require startAt.");
        }
        entity.setStatus(normalizeStatus(status, entity.isPublished(), entity.getStartAt(), entity.getEndAt()));
        if (entity.getStatus() == CarouselStatus.ARCHIVED) {
            entity.setPublished(false);
        }
        validatePinnedOverlap(entity);
        entity.setUpdatedBy(currentActor());
        entity.setVersionNumber(nextVersion(entity.getVersionNumber()));
        return toAdminDto(carouselSlideRepository.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "carouselSlides", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CarouselSlideDto publishCarousel(UUID carouselId, boolean published, Instant startAt, Instant endAt) {
        CarouselSlide entity = requireCarousel(carouselId, false);
        if (startAt != null) {
            entity.setStartAt(startAt);
        }
        if (endAt != null || entity.getEndAt() != null) {
            entity.setEndAt(endAt);
        }
        validateSchedule(entity.getStartAt(), entity.getEndAt());
        entity.setPublished(published);
        if (published && entity.getPublishedAt() == null) {
            entity.setPublishedAt(Instant.now());
        }
        entity.setStatus(normalizeStatus(entity.getStatus(), published, entity.getStartAt(), entity.getEndAt()));
        validatePinnedOverlap(entity);
        entity.setUpdatedBy(currentActor());
        entity.setVersionNumber(nextVersion(entity.getVersionNumber()));
        return toAdminDto(carouselSlideRepository.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "carouselSlides", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<CarouselSlideDto> reorderCarousels(List<CarouselReorderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("CAROUSEL_REORDER_EMPTY", "Reorder items are required.");
        }

        List<UUID> ids = items.stream().map(CarouselReorderItemRequest::id).filter(Objects::nonNull).toList();
        List<CarouselSlide> entities = carouselSlideRepository.findAllById(ids);
        if (entities.size() != ids.size()) {
            throw new NotFoundException("CAROUSEL_NOT_FOUND", "One or more carousel slides were not found.");
        }

        // Reorder updates are intentionally explicit so drag-and-drop always results in deterministic positions.
        for (CarouselReorderItemRequest item : items) {
            CarouselSlide slide = entities.stream()
                    .filter(candidate -> candidate.getId().equals(item.id()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("CAROUSEL_NOT_FOUND", "Carousel slide not found."));
            if (item.position() == null || item.position() < 0) {
                throw new BadRequestException("CAROUSEL_POSITION_INVALID", "position must be >= 0.");
            }
            slide.setPosition(item.position());
            slide.setUpdatedBy(currentActor());
            slide.setVersionNumber(nextVersion(slide.getVersionNumber()));
        }

        return carouselSlideRepository.saveAll(entities).stream()
                .sorted(Comparator.comparing(CarouselSlide::getPosition).thenComparing(CarouselSlide::getId))
                .map(this::toAdminDto)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "carouselSlides", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CarouselSlideDto duplicateCarousel(UUID carouselId) {
        CarouselSlide source = requireCarousel(carouselId, true);
        CarouselSlide copy = new CarouselSlide();
        copy.setTitle(source.getTitle() + " (Copy)");
        copy.setSlug(nextAvailableSlug(source.getSlug() + "-copy", null));
        copy.setDescription(source.getDescription());
        copy.setImageDesktop(source.getImageDesktop());
        copy.setImageMobile(source.getImageMobile());
        copy.setAltText(source.getAltText());
        copy.setLinkType(source.getLinkType());
        copy.setLinkValue(source.getLinkValue());
        copy.setOpenInNewTab(source.isOpenInNewTab());
        copy.setButtonText(source.getButtonText());
        copy.setSecondaryButtonText(source.getSecondaryButtonText());
        copy.setSecondaryLinkType(source.getSecondaryLinkType());
        copy.setSecondaryLinkValue(source.getSecondaryLinkValue());
        copy.setSecondaryOpenInNewTab(source.isSecondaryOpenInNewTab());
        copy.setPosition(nextPosition());
        copy.setStatus(CarouselStatus.DRAFT);
        copy.setVisibility(source.getVisibility());
        copy.setStartAt(source.getStartAt());
        copy.setEndAt(source.getEndAt());
        copy.setAudienceSegment(source.getAudienceSegment());
        copy.setTargetingRulesJson(source.getTargetingRulesJson());
        copy.setStoreId(source.getStoreId());
        copy.setChannelId(source.getChannelId());
        copy.setLocale(source.getLocale());
        copy.setPriority(source.getPriority());
        copy.setBackgroundStyle(source.getBackgroundStyle());
        copy.setThemeMetadataJson(source.getThemeMetadataJson());
        copy.setPublished(false);
        copy.setPinned(false);
        copy.setPreviewToken(UUID.randomUUID().toString());
        copy.setVersionNumber(1);
        copy.setAnalyticsKey(source.getAnalyticsKey());
        copy.setExperimentKey(source.getExperimentKey());
        copy.setCreatedBy(currentActor());
        copy.setUpdatedBy(currentActor());
        return toAdminDto(carouselSlideRepository.save(copy));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "carouselSlides", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<CarouselSlideDto> applyBulkAction(CarouselBulkActionRequest request) {
        List<UUID> ids = new ArrayList<>(new LinkedHashSet<>(request.ids()));
        if (ids.isEmpty()) {
            throw new BadRequestException("CAROUSEL_BULK_EMPTY", "Select at least one carousel slide.");
        }
        List<CarouselSlideDto> updated = new ArrayList<>();
        for (UUID id : ids) {
            updated.add(applyBulkAction(id, request.action()));
        }
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CarouselPreviewDto previewCarousel(UUID carouselId) {
        CarouselSlide slide = requireCarousel(carouselId, true);
        return new CarouselPreviewDto(
                toStorefrontDto(slide),
                isStorefrontVisibleNow(slide, slide.getAudienceSegment()),
                explainVisibility(slide, slide.getAudienceSegment()),
                slide.getPreviewToken()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "carouselSlides", key = "'hero:' + #storeId + ':' + #channelId + ':' + #locale + ':' + #audienceSegment + ':' + #previewToken")
    public List<StorefrontCarouselSlideDto> listStorefrontSlides(UUID storeId,
                                                                 String channelId,
                                                                 String locale,
                                                                 String audienceSegment,
                                                                 String previewToken) {
        if (hasText(previewToken)) {
            Optional<CarouselSlide> previewSlide = carouselSlideRepository.findByPreviewToken(previewToken.trim());
            if (previewSlide.isPresent() && previewSlide.get().getDeletedAt() == null) {
                return List.of(toStorefrontDto(previewSlide.get()));
            }
        }

        Specification<CarouselSlide> spec = Specification.<CarouselSlide>where((root, query, cb) -> cb.isNull(root.get("deletedAt")))
                .and((root, query, cb) -> cb.isTrue(root.get("published")))
                .and((root, query, cb) -> root.get("status").in(CarouselStatus.ACTIVE, CarouselStatus.SCHEDULED));

        if (storeId != null) {
            spec = spec.and((root, query, cb) -> cb.or(cb.isNull(root.get("storeId")), cb.equal(root.get("storeId"), storeId)));
        } else {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("storeId")));
        }
        if (hasText(channelId)) {
            String normalizedChannel = channelId.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.isNull(root.get("channelId")),
                    cb.equal(cb.lower(root.get("channelId")), normalizedChannel)
            ));
        } else {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("channelId")));
        }
        if (hasText(locale)) {
            String normalizedLocale = locale.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.isNull(root.get("locale")),
                    cb.equal(cb.lower(root.get("locale")), normalizedLocale)
            ));
        } else {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("locale")));
        }

        Sort sort = Sort.by(
                Sort.Order.desc("pinned"),
                Sort.Order.desc("priority"),
                Sort.Order.asc("position"),
                Sort.Order.desc("publishedAt"),
                Sort.Order.desc("createdAt")
        );

        return carouselSlideRepository.findAll(spec, sort).stream()
                .filter(slide -> isStorefrontVisibleNow(slide, audienceSegment))
                .map(this::toStorefrontDto)
                .toList();
    }

    private CarouselSlideDto applyBulkAction(UUID carouselId, CarouselBulkActionType action) {
        return switch (action) {
            case ACTIVATE -> changeStatus(carouselId, CarouselStatus.ACTIVE);
            case DEACTIVATE -> changeStatus(carouselId, CarouselStatus.INACTIVE);
            case ARCHIVE -> changeStatus(carouselId, CarouselStatus.ARCHIVED);
            case PUBLISH -> publishCarousel(carouselId, true, null, null);
            case UNPUBLISH -> publishCarousel(carouselId, false, null, null);
            case RESTORE -> restoreCarousel(carouselId);
            case DELETE -> {
                deleteCarousel(carouselId);
                yield toAdminDto(requireCarousel(carouselId, true));
            }
            case PIN -> updatePinned(carouselId, true);
            case UNPIN -> updatePinned(carouselId, false);
        };
    }

    private CarouselSlideDto updatePinned(UUID carouselId, boolean pinned) {
        CarouselSlide entity = requireCarousel(carouselId, false);
        entity.setPinned(pinned);
        validatePinnedOverlap(entity);
        entity.setUpdatedBy(currentActor());
        entity.setVersionNumber(nextVersion(entity.getVersionNumber()));
        return toAdminDto(carouselSlideRepository.save(entity));
    }

    private CarouselSlide requireCarousel(UUID carouselId, boolean includeDeleted) {
        CarouselSlide entity = carouselSlideRepository.findById(carouselId)
                .orElseThrow(() -> new NotFoundException("CAROUSEL_NOT_FOUND", "Carousel slide not found"));
        if (!includeDeleted && entity.getDeletedAt() != null) {
            throw new NotFoundException("CAROUSEL_NOT_FOUND", "Carousel slide not found");
        }
        return entity;
    }

    private void applyRequest(CarouselSlide entity, CarouselSlideRequest request, boolean creating) {
        entity.setTitle(request.title().trim());
        entity.setSlug(nextAvailableSlug(request.slug(), entity.getId(), request.title()));
        entity.setDescription(trimToNull(request.description()));
        entity.setImageDesktop(validateImageReference(request.imageDesktop(), "imageDesktop"));
        entity.setImageMobile(validateOptionalImageReference(request.imageMobile()));
        entity.setAltText(trimToNull(request.altText()));

        CarouselLinkType primaryLinkType = request.linkType() == null ? CarouselLinkType.INTERNAL : request.linkType();
        validateLink(primaryLinkType, request.linkValue(), request.buttonText(), "primary");
        entity.setLinkType(primaryLinkType);
        entity.setLinkValue(trimToNull(request.linkValue()));
        entity.setOpenInNewTab(Boolean.TRUE.equals(request.openInNewTab()));
        entity.setButtonText(trimToNull(request.buttonText()));

        if (hasText(request.secondaryButtonText()) || request.secondaryLinkType() != null || hasText(request.secondaryLinkValue())) {
            CarouselLinkType secondaryType = request.secondaryLinkType() == null ? CarouselLinkType.INTERNAL : request.secondaryLinkType();
            validateLink(secondaryType, request.secondaryLinkValue(), request.secondaryButtonText(), "secondary");
            entity.setSecondaryLinkType(secondaryType);
            entity.setSecondaryLinkValue(trimToNull(request.secondaryLinkValue()));
        } else {
            entity.setSecondaryLinkType(null);
            entity.setSecondaryLinkValue(null);
        }
        entity.setSecondaryButtonText(trimToNull(request.secondaryButtonText()));
        entity.setSecondaryOpenInNewTab(Boolean.TRUE.equals(request.secondaryOpenInNewTab()));

        entity.setPosition(request.position() == null ? (creating ? nextPosition() : entity.getPosition()) : Math.max(0, request.position()));
        entity.setVisibility(request.visibility() == null ? CarouselVisibility.PUBLIC : request.visibility());
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        validateSchedule(entity.getStartAt(), entity.getEndAt());

        entity.setAudienceSegment(trimToNull(request.audienceSegment()));
        entity.setTargetingRulesJson(validateJsonField(request.targetingRulesJson(), "targetingRulesJson"));
        entity.setStoreId(validateStore(request.storeId()));
        entity.setChannelId(trimToNull(request.channelId()));
        entity.setLocale(normalizeLocale(request.locale()));
        entity.setPriority(request.priority() == null ? 0 : Math.max(0, request.priority()));
        entity.setBackgroundStyle(normalizeBackgroundStyle(request.backgroundStyle()));
        entity.setThemeMetadataJson(validateJsonField(request.themeMetadataJson(), "themeMetadataJson"));
        entity.setPublished(Boolean.TRUE.equals(request.published()));
        if (entity.isPublished() && entity.getPublishedAt() == null) {
            entity.setPublishedAt(Instant.now());
        }
        entity.setPinned(Boolean.TRUE.equals(request.pinned()));
        entity.setAnalyticsKey(trimToNull(request.analyticsKey()));
        entity.setExperimentKey(trimToNull(request.experimentKey()));

        CarouselStatus requestedStatus = request.status();
        if (!creating && requestedStatus == null) {
            requestedStatus = entity.getStatus();
        }
        entity.setStatus(normalizeStatus(requestedStatus, entity.isPublished(), entity.getStartAt(), entity.getEndAt()));

        if (entity.getDeletedAt() != null) {
            entity.setPublished(false);
            entity.setStatus(CarouselStatus.ARCHIVED);
        }

        validatePinnedOverlap(entity);
    }

    private UUID validateStore(UUID storeId) {
        if (storeId == null) {
            return null;
        }
        if (!storeRepository.existsById(storeId)) {
            throw new NotFoundException("CAROUSEL_STORE_NOT_FOUND", "Referenced store was not found.");
        }
        return storeId;
    }

    private String validateJsonField(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            objectMapper.readTree(normalized);
        } catch (Exception ex) {
            throw new BadRequestException("CAROUSEL_JSON_INVALID", fieldName + " must contain valid JSON.");
        }
        return normalized;
    }

    private void validateSchedule(Instant startAt, Instant endAt) {
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new BadRequestException("CAROUSEL_SCHEDULE_INVALID", "endAt must be later than startAt.");
        }
    }

    private void validateLink(CarouselLinkType linkType, String linkValue, String buttonText, String label) {
        if (!hasText(buttonText) && !hasText(linkValue)) {
            return;
        }
        if (!hasText(linkValue)) {
            throw new BadRequestException("CAROUSEL_LINK_REQUIRED", label + " linkValue is required when button text is provided.");
        }
        String value = linkValue.trim();
        switch (linkType) {
            case EXTERNAL -> {
                if (!(value.startsWith("http://") || value.startsWith("https://"))) {
                    throw new BadRequestException("CAROUSEL_LINK_INVALID", label + " external links must start with http:// or https://.");
                }
            }
            case INTERNAL, CUSTOM -> {
                if (!value.startsWith("/")) {
                    throw new BadRequestException("CAROUSEL_LINK_INVALID", label + " internal links must start with /.");
                }
            }
            case CATEGORY, PRODUCT, COLLECTION -> {
                if (value.length() < 2) {
                    throw new BadRequestException("CAROUSEL_LINK_INVALID", label + " destination reference is required.");
                }
            }
        }
    }

    private String validateImageReference(String value, String fieldName) {
        if (!hasText(value)) {
            throw new BadRequestException("CAROUSEL_MEDIA_REQUIRED", fieldName + " is required.");
        }
        String normalized = value.trim();
        if (normalized.startsWith("/") || normalized.startsWith("http://") || normalized.startsWith("https://")) {
            if (normalized.startsWith("/") || IMAGE_REF_PATTERN.matcher(normalized).matches()) {
                return normalized;
            }
        }
        throw new BadRequestException("CAROUSEL_MEDIA_INVALID", fieldName + " must reference a valid image asset.");
    }

    private String validateOptionalImageReference(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return validateImageReference(normalized, "imageMobile");
    }

    private String normalizeLocale(String locale) {
        String normalized = trimToNull(locale);
        if (normalized == null) {
            return null;
        }
        return normalized.replace('_', '-');
    }

    private String normalizeBackgroundStyle(String backgroundStyle) {
        String normalized = trimToNull(backgroundStyle);
        return normalized == null ? "gradient" : normalized;
    }

    private CarouselStatus normalizeStatus(CarouselStatus requestedStatus, boolean published, Instant startAt, Instant endAt) {
        CarouselStatus status = requestedStatus == null ? (published ? CarouselStatus.ACTIVE : CarouselStatus.DRAFT) : requestedStatus;
        if (status == CarouselStatus.ARCHIVED) {
            return CarouselStatus.ARCHIVED;
        }
        if (published) {
            Instant now = Instant.now();
            if (startAt != null && startAt.isAfter(now)) {
                return CarouselStatus.SCHEDULED;
            }
            if (endAt != null && !now.isBefore(endAt)) {
                return CarouselStatus.INACTIVE;
            }
            if (status == CarouselStatus.DRAFT || status == CarouselStatus.SCHEDULED) {
                return CarouselStatus.ACTIVE;
            }
        } else if (requestedStatus == null) {
            return CarouselStatus.DRAFT;
        }
        return status;
    }

    private void validatePinnedOverlap(CarouselSlide candidate) {
        if (!candidate.isPinned() || !candidate.isPublished() || candidate.getDeletedAt() != null || candidate.getStatus() == CarouselStatus.ARCHIVED) {
            return;
        }

        // Featured slides are treated as an exclusive slot per store/channel/locale scope.
        Specification<CarouselSlide> spec = Specification.<CarouselSlide>where((root, query, cb) -> cb.isTrue(root.get("pinned")))
                .and((root, query, cb) -> cb.isTrue(root.get("published")))
                .and((root, query, cb) -> cb.isNull(root.get("deletedAt")))
                .and((root, query, cb) -> cb.notEqual(root.get("id"), candidate.getId() == null ? UUID.randomUUID() : candidate.getId()))
                .and(scopeEquals("storeId", candidate.getStoreId()))
                .and(scopeEqualsIgnoreCase("channelId", candidate.getChannelId()))
                .and(scopeEqualsIgnoreCase("locale", candidate.getLocale()));

        List<CarouselSlide> scopedPinnedSlides = carouselSlideRepository.findAll(spec);
        boolean overlaps = scopedPinnedSlides.stream().anyMatch(existing -> schedulesOverlap(existing.getStartAt(), existing.getEndAt(), candidate.getStartAt(), candidate.getEndAt()));
        if (overlaps) {
            throw new BadRequestException(
                    "CAROUSEL_PIN_CONFLICT",
                    "Pinned slides cannot have overlapping publish windows in the same store/channel/locale scope."
            );
        }
    }

    private Specification<CarouselSlide> scopeEquals(String field, UUID value) {
        return (root, query, cb) -> value == null ? cb.isNull(root.get(field)) : cb.equal(root.get(field), value);
    }

    private Specification<CarouselSlide> scopeEqualsIgnoreCase(String field, String value) {
        return (root, query, cb) -> {
            if (!hasText(value)) {
                return cb.isNull(root.get(field));
            }
            return cb.equal(cb.lower(root.get(field)), value.trim().toLowerCase(Locale.ROOT));
        };
    }

    private boolean schedulesOverlap(Instant leftStart, Instant leftEnd, Instant rightStart, Instant rightEnd) {
        Instant normalizedLeftStart = leftStart == null ? Instant.EPOCH : leftStart;
        Instant normalizedRightStart = rightStart == null ? Instant.EPOCH : rightStart;
        Instant normalizedLeftEnd = leftEnd == null ? Instant.parse("9999-12-31T23:59:59Z") : leftEnd;
        Instant normalizedRightEnd = rightEnd == null ? Instant.parse("9999-12-31T23:59:59Z") : rightEnd;
        return normalizedLeftStart.isBefore(normalizedRightEnd) && normalizedRightStart.isBefore(normalizedLeftEnd);
    }

    private int nextPosition() {
        return Optional.ofNullable(carouselSlideRepository.findMaxPosition()).orElse(0) + 1;
    }

    private int nextVersion(Integer currentVersion) {
        return currentVersion == null ? 1 : currentVersion + 1;
    }

    private String nextAvailableSlug(String requestedSlug, UUID currentId, String fallbackTitle) {
        String base = slugify(hasText(requestedSlug) ? requestedSlug : fallbackTitle);
        if (!hasText(base)) {
            base = "carousel-slide";
        }
        return nextAvailableSlug(base, currentId);
    }

    private String nextAvailableSlug(String base, UUID currentId) {
        String slug = slugify(base);
        if (!hasText(slug)) {
            slug = "carousel-slide";
        }
        String candidate = slug;
        int suffix = 2;
        while (slugExists(candidate, currentId)) {
            candidate = slug + "-" + suffix;
            suffix += 1;
        }
        return candidate;
    }

    private boolean slugExists(String slug, UUID currentId) {
        return currentId == null
                ? carouselSlideRepository.existsBySlugIgnoreCase(slug)
                : carouselSlideRepository.existsBySlugIgnoreCaseAndIdNot(slug, currentId);
    }

    private String slugify(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private boolean isStorefrontVisibleNow(CarouselSlide slide, String audienceSegment) {
        return explainVisibility(slide, audienceSegment).isEmpty();
    }

    private List<String> explainVisibility(CarouselSlide slide, String audienceSegment) {
        List<String> reasons = new ArrayList<>();

        // Storefront visibility is centralized here so admin preview and public delivery use the same scheduling rules.
        if (slide.getDeletedAt() != null) {
            reasons.add("Slide is soft-deleted.");
        }
        if (!slide.isPublished()) {
            reasons.add("Slide is not published.");
        }
        if (slide.getStatus() == CarouselStatus.DRAFT) {
            reasons.add("Draft slides are never rendered on the storefront.");
        }
        if (slide.getStatus() == CarouselStatus.INACTIVE) {
            reasons.add("Inactive slides are hidden from the storefront.");
        }
        if (slide.getStatus() == CarouselStatus.ARCHIVED) {
            reasons.add("Archived slides are hidden from the storefront.");
        }
        Instant now = Instant.now();
        if (slide.getStartAt() != null && now.isBefore(slide.getStartAt())) {
            reasons.add("Publish window has not started yet.");
        }
        if (slide.getEndAt() != null && !now.isBefore(slide.getEndAt())) {
            reasons.add("Slide publish window has expired.");
        }
        if (!isVisibilityAllowed(slide.getVisibility(), audienceSegment)) {
            reasons.add("Visibility rules do not match the requested audience.");
        }
        if (hasText(slide.getAudienceSegment())) {
            if (!hasText(audienceSegment) || !slide.getAudienceSegment().trim().equalsIgnoreCase(audienceSegment.trim())) {
                reasons.add("Audience segment does not match.");
            }
        }
        return reasons;
    }

    private boolean isVisibilityAllowed(CarouselVisibility visibility, String audienceSegment) {
        if (visibility == null || visibility == CarouselVisibility.PUBLIC) {
            return true;
        }
        if (visibility == CarouselVisibility.HIDDEN) {
            return false;
        }
        if (!hasText(audienceSegment)) {
            return false;
        }
        String normalized = audienceSegment.trim().toUpperCase(Locale.ROOT);
        return switch (visibility) {
            case AUTHENTICATED -> normalized.equals("AUTHENTICATED") || normalized.equals("MEMBER");
            case B2B -> normalized.equals("B2B");
            default -> false;
        };
    }

    private CarouselSlideDto toAdminDto(CarouselSlide entity) {
        return new CarouselSlideDto(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getImageDesktop(),
                entity.getImageMobile(),
                entity.getAltText(),
                entity.getLinkType(),
                entity.getLinkValue(),
                entity.isOpenInNewTab(),
                entity.getButtonText(),
                entity.getSecondaryButtonText(),
                entity.getSecondaryLinkType(),
                entity.getSecondaryLinkValue(),
                entity.isSecondaryOpenInNewTab(),
                entity.getPosition(),
                entity.getStatus(),
                entity.getVisibility(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getAudienceSegment(),
                entity.getTargetingRulesJson(),
                entity.getStoreId(),
                entity.getChannelId(),
                entity.getLocale(),
                entity.getPriority(),
                entity.getBackgroundStyle(),
                entity.getThemeMetadataJson(),
                entity.isPublished(),
                entity.getPublishedAt(),
                entity.isPinned(),
                entity.getVersionNumber(),
                entity.getAnalyticsKey(),
                entity.getExperimentKey(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt(),
                entity.getDeletedBy(),
                entity.getPreviewToken(),
                isStorefrontVisibleNow(entity, entity.getAudienceSegment())
        );
    }

    private StorefrontCarouselSlideDto toStorefrontDto(CarouselSlide entity) {
        return new StorefrontCarouselSlideDto(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getImageDesktop(),
                entity.getImageMobile(),
                entity.getAltText(),
                entity.getLinkType(),
                entity.getLinkValue(),
                entity.isOpenInNewTab(),
                entity.getButtonText(),
                entity.getSecondaryButtonText(),
                entity.getSecondaryLinkType(),
                entity.getSecondaryLinkValue(),
                entity.isSecondaryOpenInNewTab(),
                entity.getBackgroundStyle(),
                entity.getThemeMetadataJson(),
                entity.getLocale(),
                entity.getPriority(),
                entity.getPosition(),
                entity.getAnalyticsKey(),
                entity.getExperimentKey(),
                entity.getAudienceSegment(),
                entity.getTargetingRulesJson()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String currentActor() {
        try {
            String email = SecurityUtils.currentEmail();
            return hasText(email) ? email.trim() : "system";
        } catch (Exception ex) {
            return "system";
        }
    }
}
