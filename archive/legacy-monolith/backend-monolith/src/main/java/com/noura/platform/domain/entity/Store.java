package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.StoreServiceType;
import com.noura.platform.domain.enums.StoreStatus;
import com.noura.platform.domain.enums.StoreType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedBy;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "stores")
public class Store extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "store_code", nullable = false, length = 80)
    private String storeCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 255)
    private String slug;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String zipCode;

    @Column(nullable = false)
    private String country;

    @Column(name = "country_code", length = 12)
    private String countryCode;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "service_radius_meters")
    private Integer serviceRadiusMeters;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private StoreType type = StoreType.MERCHANT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StoreStatus status = StoreStatus.ACTIVE;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(name = "store_services", joinColumns = @JoinColumn(name = "store_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "service")
    private Set<StoreServiceType> services = new HashSet<>();

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingFee;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal freeShippingThreshold;

    @Column(name = "updated_by", length = 255)
    @LastModifiedBy
    private String updatedBy;

    @PrePersist
    @PreUpdate
    private void normalizeStoreState() {
        if (status == null) {
            status = StoreStatus.ACTIVE;
        }
        active = status == StoreStatus.ACTIVE;
        if (merchantId == null) {
            return;
        }
        if (countryCode != null) {
            countryCode = countryCode.trim().toUpperCase();
        }
        if (contactEmail != null) {
            contactEmail = contactEmail.trim().toLowerCase();
        }
    }
}
