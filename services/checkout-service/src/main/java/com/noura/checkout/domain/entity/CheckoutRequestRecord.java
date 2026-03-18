package com.noura.checkout.domain.entity;

import com.noura.checkout.domain.enums.CheckoutRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Persistent idempotency record for checkout place-order commands.
 */
@Getter
@Setter
@Entity
@Table(name = "checkout_request_records")
public class CheckoutRequestRecord extends AuditableEntity {

    @Column(name = "customer_ref", nullable = false, length = 180)
    private String customerRef;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private CheckoutRequestStatus status;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "request_payload_json")
    private String requestPayloadJson;

    @Column(name = "response_payload_json")
    private String responsePayloadJson;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "failure_message", length = 600)
    private String failureMessage;
}

