package com.company.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username may contain letters, numbers, dot, underscore, and dash only")
        String username,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        Set<@NotBlank String> roleCodes
) {
}
