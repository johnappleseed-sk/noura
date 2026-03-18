package com.noura.customer.domain.entity;

import com.noura.customer.domain.enums.AddressValidationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Address entity owned by a customer profile.
 */
@Getter
@Setter
@Entity
@Table(name = "customer_addresses")
public class CustomerAddress extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @Column(name = "customer_id", insertable = false, updatable = false)
    private UUID customerId;

    @Column(name = "label", length = 80)
    private String label;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone", length = 60)
    private String phone;

    @Column(name = "line1", nullable = false, length = 255)
    private String line1;

    @Column(name = "line2", length = 255)
    private String line2;

    @Column(name = "district", length = 120)
    private String district;

    @Column(name = "city", nullable = false, length = 120)
    private String city;

    @Column(name = "state_province", nullable = false, length = 120)
    private String stateProvince;

    @Column(name = "postal_code", nullable = false, length = 30)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 8)
    private String countryCode;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "accuracy_meters")
    private Integer accuracyMeters;

    @Column(name = "place_id", length = 220)
    private String placeId;

    @Column(name = "formatted_address", length = 1024)
    private String formattedAddress;

    @Column(name = "delivery_instructions", length = 600)
    private String deliveryInstructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 32)
    private AddressValidationStatus validationStatus = AddressValidationStatus.UNVERIFIED;

    @Column(name = "default_shipping", nullable = false)
    private boolean defaultShipping;

    @Column(name = "default_billing", nullable = false)
    private boolean defaultBilling;
}
