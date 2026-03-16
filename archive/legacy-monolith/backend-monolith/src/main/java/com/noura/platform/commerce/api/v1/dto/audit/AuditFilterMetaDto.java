package com.noura.platform.commerce.api.v1.dto.audit;

import java.util.List;

public record AuditFilterMetaDto(
        List<String> actionTypes,
        List<String> targetTypes
) {
}
