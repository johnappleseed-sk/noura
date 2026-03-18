package com.noura.customer.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Customer profile aggregate root for account identity and defaults.
 */
@Getter
@Setter
@Entity
@Table(name = "customer_profiles")
public class CustomerProfile extends AuditableEntity {

    @Column(name = "external_subject", nullable = false, unique = true, length = 180)
    private String externalSubject;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone", length = 60)
    private String phone;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "default_shipping_address_id")
    private UUID defaultShippingAddressId;

    @Column(name = "default_billing_address_id")
    private UUID defaultBillingAddressId;
}
