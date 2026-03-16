package com.company.platform.auth.dto;

import com.company.platform.auth.enums.UserStatus;

import java.util.Set;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String username,
        String email,
        UserStatus status,
        Set<String> roles,
        Set<String> permissions
) {
}
