package com.noura.platform.dto.merchant;

import com.noura.platform.domain.enums.MerchantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateMerchantRequest(
        @NotBlank @Size(max = 80) String merchantCode,
        @NotBlank @Size(max = 255) String legalName,
        @NotBlank @Size(max = 255) String displayName,
        @Email String email,
        String phone,
        @Size(max = 12) String countryCode,
        LocalDate contractStartAt,
        LocalDate contractEndAt,
        String notes,
        MerchantStatus status
) {
}
