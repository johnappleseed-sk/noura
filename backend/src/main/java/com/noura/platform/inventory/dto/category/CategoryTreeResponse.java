package com.noura.platform.inventory.dto.category;

import java.util.List;

public record CategoryTreeResponse(
        String id,
        String parentId,
        String categoryCode,
        String name,
        int level,
        int sortOrder,
        boolean active,
        List<CategoryTreeResponse> children
) {
}
