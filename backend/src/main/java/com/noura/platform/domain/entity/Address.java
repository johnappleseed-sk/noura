package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "addresses")
public class Address extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    private String label;
    private String fullName;
    private String phone;
    private String line1;
    private String line2;
    private String district;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    private Integer accuracyMeters;

    @Column(length = 220)
    private String placeId;

    @Column(length = 1024)
    private String formattedAddress;

    @Column(length = 600)
    private String deliveryInstructions;

    @Column(nullable = false)
    private boolean isDefaultAddress;
}
