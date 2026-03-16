package com.noura.platform.dto.storefront;

import java.time.LocalDateTime;

public record CustomerAddressDto(Long id,
                                 String label,
                                 String recipientName,
                                 String phone,
                                 String line1,
                                 String line2,
                                 String district,
                                 String city,
                                 String stateProvince,
                                 String postalCode,
                                 String countryCode,
                                 boolean defaultShipping,
                                 boolean defaultBilling,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
}
