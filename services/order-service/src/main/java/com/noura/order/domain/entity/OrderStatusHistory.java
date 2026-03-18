package com.noura.order.domain.entity;

import com.noura.order.domain.enums.OrderStatus;
import com.noura.order.domain.enums.RefundStatus;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Status transition history entry for auditability.
 */
@Getter
@Setter
@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private OrderRecord order;

    @Column(name = "order_id", insertable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private OrderStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 32)
    private RefundStatus refundStatus = RefundStatus.NONE;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "note", length = 600)
    private String note;

    @Column(name = "changed_by", length = 180)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}

