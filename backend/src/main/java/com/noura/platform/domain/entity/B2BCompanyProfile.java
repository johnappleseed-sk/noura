package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "b2b_company_profiles")
public class B2BCompanyProfile extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private UserAccount user;

    @Column(nullable = false)
    private String companyName;

    private String taxId;
    private String costCenter;
    private String approvalEmail;

    @Column(nullable = false)
    private boolean approvalRequired;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal approvalThreshold;
}
