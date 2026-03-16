package com.noura.platform.commerce.marketplace.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps a local product/SKU to a marketplace listing.
 */
@Entity
@Table(name = "marketplace_product_mappings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"channel_id", "product_id", "variant_id"}))
public class MarketplaceProductMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private MarketplaceChannel channel;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    // External identifiers on the marketplace
    @Column(length = 100)
    private String externalProductId;

    @Column(length = 100)
    private String externalVariantId;

    @Column(length = 100)
    private String externalSku;

    @Column(length = 50)
    private String asin; // Amazon ASIN

    @Column(length = 50)
    private String ean;

    @Column(length = 50)
    private String upc;

    // Listing status on the marketplace
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status = ListingStatus.PENDING;

    // External listing URL
    @Column(length = 500)
    private String listingUrl;

    // Sync status
    @Column
    private LocalDateTime lastSyncedAt;

    @Column(length = 500)
    private String lastSyncError;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // === Getters and Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MarketplaceChannel getChannel() { return channel; }
    public void setChannel(MarketplaceChannel channel) { this.channel = channel; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    public String getExternalProductId() { return externalProductId; }
    public void setExternalProductId(String externalProductId) { this.externalProductId = externalProductId; }

    public String getExternalVariantId() { return externalVariantId; }
    public void setExternalVariantId(String externalVariantId) { this.externalVariantId = externalVariantId; }

    public String getExternalSku() { return externalSku; }
    public void setExternalSku(String externalSku) { this.externalSku = externalSku; }

    public String getAsin() { return asin; }
    public void setAsin(String asin) { this.asin = asin; }

    public String getEan() { return ean; }
    public void setEan(String ean) { this.ean = ean; }

    public String getUpc() { return upc; }
    public void setUpc(String upc) { this.upc = upc; }

    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }

    public String getListingUrl() { return listingUrl; }
    public void setListingUrl(String listingUrl) { this.listingUrl = listingUrl; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public String getLastSyncError() { return lastSyncError; }
    public void setLastSyncError(String lastSyncError) { this.lastSyncError = lastSyncError; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
