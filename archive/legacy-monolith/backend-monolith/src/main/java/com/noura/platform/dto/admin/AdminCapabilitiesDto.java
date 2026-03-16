package com.noura.platform.dto.admin;

import java.util.List;
import java.util.Map;

public record AdminCapabilitiesDto(
        List<String> roles,
        Map<String, Boolean> capabilities
) {
}
