package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.FulfillmentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "carts")
public class Cart extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    // Draft checkout data captured during multi-step checkout.
    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_method")
    private FulfillmentMethod fulfillmentMethod;

    @Column(name = "shipping_address_snapshot", columnDefinition = "TEXT")
    private String shippingAddressSnapshot;

    @Column(name = "payment_reference", columnDefinition = "TEXT")
    private String paymentReference;

    @Column(name = "coupon_code", length = 255)
    private String couponCode;

    @Column(name = "b2b_invoice")
    private boolean b2bInvoice;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;
}
