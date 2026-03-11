package com.noura.platform.dto.runtime;

import java.util.Map;

public record RuntimeFeaturesDto(
        String contractVersion,
        Map<String, Boolean> features,
        Map<String, String> messages
) {
}

