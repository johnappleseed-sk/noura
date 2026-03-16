package com.noura.platform.commerce.customers.domain;

public record StorefrontCustomerPrincipal(Long id, String email, String firstName, String lastName) {
    public String displayName() {
        if (firstName != null && !firstName.isBlank()) {
            return firstName;
        }
        if (lastName != null && !lastName.isBlank()) {
            return lastName;
        }
        return email;
    }
}

