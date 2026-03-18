package com.noura.shipping.domain.entity;

import com.noura.shipping.domain.enums.MerchantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Merchant compatibility record used by admin-web network pages.
 */
@Getter
@Setter
@Entity
@Table(name = "merchant_records")
public class MerchantRecord extends AuditableEntity {

    @Column(name = "merchant_code", nullable = false, unique = true, length = 64)
    private String merchantCode;

    @Column(name = "legal_name", nullable = false, length = 180)
    private String legalName;

    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    @Column(length = 180)
    private String email;

    @Column(length = 64)
    private String phone;

    @Column(name = "country_code", length = 16)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MerchantStatus status;

    @Column(name = "contract_start_at")
    private Instant contractStartAt;

    @Column(name = "contract_end_at")
    private Instant contractEndAt;

    @Column(length = 2000)
    private String notes;
}
