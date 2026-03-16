package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.MerchantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedBy;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Partner/merchant organization onboarded under a contract.
 */
@Getter
@Setter
@Entity
@Table(name = "merchants")
public class Merchant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "merchant_code", nullable = false, length = 80)
    private String merchantCode;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "country_code", length = 12)
    private String countryCode;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "primary_email", length = 255)
    private String email;

    @Column(name = "primary_phone", length = 40)
    private String phone;

    @Column(name = "contract_start_at")
    private LocalDate contractStartAt;

    @Column(name = "contract_end_at")
    private LocalDate contractEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MerchantStatus status = MerchantStatus.DRAFT;

    @Column(length = 1000)
    private String notes;

    @LastModifiedBy
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    private String normalizedCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizedText(String value) {
        return value == null ? null : value.trim();
    }

    private String syncName(String value) {
        return value == null || value.isBlank() ? "UNNAMED MERCHANT" : value.trim();
    }

    private String buildFallbackMerchantCode() {
        return "MER-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * Backfills legacy and canonical fields for compatibility.
     */
    @PrePersist
    @PreUpdate
    public void syncDisplayNameAndLegacyAliases() {
        String canonicalDisplay = syncName(displayName);
        String canonicalName = syncName(name);
        displayName = canonicalDisplay;
        name = canonicalName;
        legalName = isBlank(legalName) ? canonicalName : legalName.trim();
        merchantCode = normalizedCode(isBlank(merchantCode) ? buildFallbackMerchantCode() : merchantCode);
        email = normalizedText(email);
        phone = normalizedText(phone);
        countryCode = isBlank(countryCode) ? null : countryCode.trim().toUpperCase();
        notes = normalizedText(notes);
    }

    @PrePersist
    public void syncCanonicalFieldsForInsert() {
        if (contractEndAt != null && contractStartAt != null && contractEndAt.isBefore(contractStartAt)) {
            throw new IllegalArgumentException("Contract end date must be on or after contract start date");
        }
    }

    public String getPrimaryEmail() {
        return email;
    }

    public void setPrimaryEmail(String primaryEmail) {
        this.email = trim(primaryEmail);
    }

    public String getPrimaryPhone() {
        return phone;
    }

    public void setPrimaryPhone(String primaryPhone) {
        this.phone = trim(primaryPhone);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
