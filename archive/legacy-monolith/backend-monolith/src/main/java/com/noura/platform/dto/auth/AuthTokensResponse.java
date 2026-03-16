package com.noura.platform.dto.auth;

import com.noura.platform.domain.enums.RoleType;

import java.util.Set;
import java.util.UUID;

public record AuthTokensResponse(
        UUID userId,
        String email,
        String fullName,
        Set<RoleType> roles,
        String accessToken,
        String refreshToken
) {
}
