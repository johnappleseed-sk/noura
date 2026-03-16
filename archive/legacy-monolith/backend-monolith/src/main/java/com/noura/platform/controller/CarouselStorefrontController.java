package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.carousel.StorefrontCarouselSlideDto;
import com.noura.platform.service.CarouselService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Carousel Storefront")
@RequestMapping("${app.api.version-prefix:/api/v1}/carousels")
public class CarouselStorefrontController {

    private final CarouselService carouselService;

    @Operation(summary = "Fetch storefront hero slides that are currently eligible for display")
    @GetMapping("/hero")
    public ApiResponse<List<StorefrontCarouselSlideDto>> hero(@RequestParam(required = false) UUID storeId,
                                                              @RequestParam(required = false) String channelId,
                                                              @RequestParam(required = false) String locale,
                                                              @RequestParam(required = false) String audienceSegment,
                                                              @RequestParam(required = false) String previewToken,
                                                              HttpServletRequest http) {
        return ApiResponse.ok(
                "Hero carousel slides",
                carouselService.listStorefrontSlides(storeId, channelId, locale, audienceSegment, previewToken),
                http.getRequestURI()
        );
    }
}
