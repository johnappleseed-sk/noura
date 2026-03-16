package com.noura.platform.dto.merchant;

import com.noura.platform.common.api.PageResponse;

import java.util.List;

public record MerchantListResponse(
        List<MerchantResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static MerchantListResponse from(PageResponse<MerchantResponse> page) {
        return new MerchantListResponse(
                page.getContent(),
                page.getPage(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
