package com.noura.platform.dto.auth;

import com.noura.platform.commerce.entity.UserRole;

import java.util.List;

public record RegisterResult(Long userId,
                             String username,
                             String email,
                             UserRole role,
                             List<String> permissions) {
}
