package com.company.platform.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        CurrentUserResponse user
) {
}
