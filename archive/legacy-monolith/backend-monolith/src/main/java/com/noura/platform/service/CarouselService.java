package com.noura.platform.service;

import com.noura.platform.domain.enums.CarouselStatus;
import com.noura.platform.dto.carousel.CarouselBulkActionRequest;
import com.noura.platform.dto.carousel.CarouselPreviewDto;
import com.noura.platform.dto.carousel.CarouselReorderItemRequest;
import com.noura.platform.dto.carousel.CarouselSlideDto;
import com.noura.platform.dto.carousel.CarouselSlideRequest;
import com.noura.platform.dto.carousel.StorefrontCarouselSlideDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CarouselService {
    Page<CarouselSlideDto> listAdminCarousels(String q,
                                             CarouselStatus status,
                                             Boolean published,
                                             UUID storeId,
                                             String locale,
                                             Boolean includeDeleted,
                                             Instant startFrom,
                                             Instant startTo,
                                             Pageable pageable);

    CarouselSlideDto getAdminCarousel(UUID carouselId, boolean includeDeleted);

    CarouselSlideDto createCarousel(CarouselSlideRequest request);

    CarouselSlideDto updateCarousel(UUID carouselId, CarouselSlideRequest request);

    void deleteCarousel(UUID carouselId);

    CarouselSlideDto restoreCarousel(UUID carouselId);

    CarouselSlideDto changeStatus(UUID carouselId, CarouselStatus status);

    CarouselSlideDto publishCarousel(UUID carouselId, boolean published, Instant startAt, Instant endAt);

    List<CarouselSlideDto> reorderCarousels(List<CarouselReorderItemRequest> items);

    CarouselSlideDto duplicateCarousel(UUID carouselId);

    List<CarouselSlideDto> applyBulkAction(CarouselBulkActionRequest request);

    CarouselPreviewDto previewCarousel(UUID carouselId);

    List<StorefrontCarouselSlideDto> listStorefrontSlides(UUID storeId,
                                                          String channelId,
                                                          String locale,
                                                          String audienceSegment,
                                                          String previewToken);
}
