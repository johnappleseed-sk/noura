package com.noura.platform.commerce.b2b.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * B2B Purchase Order from a company.
 * Supports order approval workflow and invoicing.
 */
@Entity
@Table(name = "b2b_purchase_orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private CompanyContact orderedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private CompanyContact approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private POStatus status = POStatus.DRAFT;

    // Company's reference number
    @Column(length = 100)
    private String customerReference;

    // Amounts
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Payment
    @Column
    private LocalDate paymentDueDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    // Shipping
    @Column(length = 255)
    private String shippingAddressLine1;

    @Column(length = 255)
    private String shippingAddressLine2;

    @Column(length = 100)
    private String shippingCity;

    @Column(length = 100)
    private String shippingState;

    @Column(length = 20)
    private String shippingPostalCode;

    @Column(length = 2)
    private String shippingCountryCode;

    @Column(columnDefinition = "TEXT")
    private String shippingInstructions;

    // Notes
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String internalNotes;

    // Line items
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column
    private LocalDateTime submittedAt;

    @Column
    private LocalDateTime approvedAt;

    @Column
    private LocalDateTime shippedAt;

    // === Computed ===

    public BigDecimal getBalanceDue() {
        return totalAmount.subtract(amountPaid);
    }

    public boolean isPaid() {
        return amountPaid.compareTo(totalAmount) >= 0;
    }

    // === Getters and Setters (abbreviated for space) ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public CompanyContact getOrderedBy() { return orderedBy; }
    public void setOrderedBy(CompanyContact orderedBy) { this.orderedBy = orderedBy; }

    public CompanyContact getApprovedBy() { return approvedBy; }
    public void setApprovedBy(CompanyContact approvedBy) { this.approvedBy = approvedBy; }

    public POStatus getStatus() { return status; }
    public void setStatus(POStatus status) { this.status = status; }

    public String getCustomerReference() { return customerReference; }
    public void setCustomerReference(String customerReference) { this.customerReference = customerReference; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public void setPaymentDueDate(LocalDate paymentDueDate) { this.paymentDueDate = paymentDueDate; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public String getShippingAddressLine1() { return shippingAddressLine1; }
    public void setShippingAddressLine1(String v) { this.shippingAddressLine1 = v; }

    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String v) { this.shippingCity = v; }

    public String getShippingState() { return shippingState; }
    public void setShippingState(String v) { this.shippingState = v; }

    public String getShippingPostalCode() { return shippingPostalCode; }
    public void setShippingPostalCode(String v) { this.shippingPostalCode = v; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }

    public List<PurchaseOrderItem> getItems() { return items; }
    public void setItems(List<PurchaseOrderItem> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime v) { this.submittedAt = v; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime v) { this.approvedAt = v; }
}
