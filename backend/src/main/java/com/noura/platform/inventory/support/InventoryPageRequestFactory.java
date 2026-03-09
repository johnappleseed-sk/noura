package com.noura.platform.inventory.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class InventoryPageRequestFactory {

    private InventoryPageRequestFactory() {
    }

    public static Pageable of(int page,
                              int size,
                              String sortBy,
                              String direction,
                              Set<String> allowedSorts,
                              String defaultSort) {
        String safeSort = allowedSorts.contains(sortBy) ? sortBy : defaultSort;
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(sortDirection, safeSort));
    }
}
