package com.noura.platform.dto.catalog;

import com.noura.platform.domain.enums.CategoryChangeAction;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CategoryChangeSubmitRequest(
        UUID categoryId,
        @NotNull CategoryChangeAction action,
        Map<String, Object> payload,
        String reason
) {
}
