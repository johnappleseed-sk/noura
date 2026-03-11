package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.AddressValidationStatus;
import com.noura.platform.domain.enums.FulfillmentMethod;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.domain.enums.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "address_id")
    private UUID addressId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentMethod fulfillmentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus refundStatus = RefundStatus.NONE;

    private String shippingAddressSnapshot;
    private String paymentReference;
    private String couponCode;

    @Lob
    @Column(name = "location_snapshot_json")
    private String locationSnapshotJson;

    @Column(name = "matched_service_area_id")
    private UUID matchedServiceAreaId;

    @Column(name = "eligibility_reason", length = 80)
    private String eligibilityReason;

    @Column(name = "delivery_latitude", precision = 10, scale = 7)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", precision = 10, scale = 7)
    private BigDecimal deliveryLongitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_validation_status", length = 32)
    private AddressValidationStatus addressValidationStatus;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;
}
