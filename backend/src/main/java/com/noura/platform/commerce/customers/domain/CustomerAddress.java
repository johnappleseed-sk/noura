package com.noura.platform.commerce.customers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "customer_address", indexes = {
        @Index(name = "idx_customer_address_account", columnList = "customer_account_id")
})
public class CustomerAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_account_id", nullable = false)
    private CustomerAccount customerAccount;

    @Column(length = 80)
    private String label;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false, length = 255)
    private String line1;

    @Column(length = 255)
    private String line2;

    @Column(length = 120)
    private String district;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(name = "state_province", length = 120)
    private String stateProvince;

    @Column(name = "postal_code", length = 40)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "default_shipping", nullable = false)
    private Boolean defaultShipping = false;

    @Column(name = "default_billing", nullable = false)
    private Boolean defaultBilling = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        normalize();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (label != null) {
            label = label.trim();
            if (label.isEmpty()) {
                label = null;
            }
        }
        if (recipientName != null) {
            recipientName = recipientName.trim();
        }
        if (phone != null) {
            phone = phone.trim();
            if (phone.isEmpty()) {
                phone = null;
            }
        }
        if (line1 != null) {
            line1 = line1.trim();
        }
        if (line2 != null) {
            line2 = line2.trim();
            if (line2.isEmpty()) {
                line2 = null;
            }
        }
        if (district != null) {
            district = district.trim();
            if (district.isEmpty()) {
                district = null;
            }
        }
        if (city != null) {
            city = city.trim();
        }
        if (stateProvince != null) {
            stateProvince = stateProvince.trim();
            if (stateProvince.isEmpty()) {
                stateProvince = null;
            }
        }
        if (postalCode != null) {
            postalCode = postalCode.trim();
            if (postalCode.isEmpty()) {
                postalCode = null;
            }
        }
        if (countryCode != null) {
            countryCode = countryCode.trim().toUpperCase();
        }
        if (defaultShipping == null) {
            defaultShipping = false;
        }
        if (defaultBilling == null) {
            defaultBilling = false;
        }
    }
}
