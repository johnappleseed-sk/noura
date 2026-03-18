package com.noura.customer.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Customer-owned saved payment method record.
 */
@Getter
@Setter
@Entity
@Table(name = "customer_payment_methods")
public class CustomerPaymentMethod extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @Column(name = "method_type", nullable = false, length = 40)
    private String methodType;

    @Column(name = "provider", nullable = false, length = 80)
    private String provider;

    @Column(name = "tokenized_reference", nullable = false, length = 255)
    private String tokenizedReference;

    @Column(name = "default_method", nullable = false)
    private boolean defaultMethod;
}
