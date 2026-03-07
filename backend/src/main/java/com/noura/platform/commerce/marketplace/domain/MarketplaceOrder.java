package com.noura.platform.commerce.marketplace.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order received from a marketplace channel.
 */
@Entity
@Table(name = "marketplace_orders")
public class MarketplaceOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private MarketplaceChannel channel;

    // External order identifier on the marketplace
    @Column(nullable = false, length = 100)
    private String externalOrderId;

    // Link to internal order (after import)
    @Column(name = "internal_order_id")
    private Long internalOrderId;

    // Order status from marketplace
    @Column(length = 50)
    private String externalStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportStatus importStatus = ImportStatus.PENDING;

    // Customer info from marketplace
    @Column(length = 200)
    private String buyerName;

    @Column(length = 255)
    private String buyerEmail;

    // Shipping address
    @Column(length = 255)
    private String shipName;

    @Column(length = 255)
    private String shipAddressLine1;

    @Column(length = 255)
    private String shipAddressLine2;

    @Column(length = 100)
    private String shipCity;

    @Column(length = 100)
    private String shipState;

    @Column(length = 20)
    private String shipPostalCode;

    @Column(length = 2)
    private String shipCountryCode;

    @Column(length = 30)
    private String shipPhone;

    // Order totals
    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 15, scale = 2)
    private BigDecimal shippingAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 3)
    private String currencyCode;

    // Shipping info
    @Column(length = 100)
    private String shippingService;

    @Column
    private LocalDateTime shipByDate;

    // Raw order data from marketplace
    @Column(columnDefinition = "JSON")
    private String rawOrderData;

    // Timestamps
    @Column
    private LocalDateTime orderDateOnMarketplace;

    @Column
    private LocalDateTime importedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 500)
    private String importError;

    // === Getters and Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MarketplaceChannel getChannel() { return channel; }
    public void setChannel(MarketplaceChannel channel) { this.channel = channel; }

    public String getExternalOrderId() { return externalOrderId; }
    public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }

    public Long getInternalOrderId() { return internalOrderId; }
    public void setInternalOrderId(Long internalOrderId) { this.internalOrderId = internalOrderId; }

    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }

    public ImportStatus getImportStatus() { return importStatus; }
    public void setImportStatus(ImportStatus importStatus) { this.importStatus = importStatus; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }

    public String getShipName() { return shipName; }
    public void setShipName(String shipName) { this.shipName = shipName; }

    public String getShipAddressLine1() { return shipAddressLine1; }
    public void setShipAddressLine1(String v) { this.shipAddressLine1 = v; }

    public String getShipCity() { return shipCity; }
    public void setShipCity(String v) { this.shipCity = v; }

    public String getShipState() { return shipState; }
    public void setShipState(String v) { this.shipState = v; }

    public String getShipPostalCode() { return shipPostalCode; }
    public void setShipPostalCode(String v) { this.shipPostalCode = v; }

    public String getShipCountryCode() { return shipCountryCode; }
    public void setShipCountryCode(String v) { this.shipCountryCode = v; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getShippingService() { return shippingService; }
    public void setShippingService(String shippingService) { this.shippingService = shippingService; }

    public LocalDateTime getShipByDate() { return shipByDate; }
    public void setShipByDate(LocalDateTime shipByDate) { this.shipByDate = shipByDate; }

    public String getRawOrderData() { return rawOrderData; }
    public void setRawOrderData(String rawOrderData) { this.rawOrderData = rawOrderData; }

    public LocalDateTime getOrderDateOnMarketplace() { return orderDateOnMarketplace; }
    public void setOrderDateOnMarketplace(LocalDateTime v) { this.orderDateOnMarketplace = v; }

    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getImportError() { return importError; }
    public void setImportError(String importError) { this.importError = importError; }
}
