package com.noura.platform.commerce.marketplace.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a sales channel connection (Amazon, eBay, Shopify, Walmart, etc.).
 */
@Entity
@Table(name = "marketplace_channels")
public class MarketplaceChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChannelType type;

    @Column(nullable = false)
    private boolean active = true;

    // API credentials (encrypted in production)
    @Column(length = 500)
    private String apiKey;

    @Column(length = 500)
    private String apiSecret;

    @Column(length = 500)
    private String accessToken;

    @Column(length = 500)
    private String refreshToken;

    @Column
    private LocalDateTime tokenExpiresAt;

    // Seller/merchant account ID on the platform
    @Column(length = 100)
    private String merchantId;

    // Store/shop ID on the platform
    @Column(length = 100)
    private String storeId;

    // API endpoint (for platforms with multiple regions)
    @Column(length = 255)
    private String apiEndpoint;

    // Sync settings as JSON
    @Column(columnDefinition = "JSON")
    private String syncSettings;

    // Last sync timestamps
    @Column
    private LocalDateTime lastProductSync;

    @Column
    private LocalDateTime lastOrderSync;

    @Column
    private LocalDateTime lastInventorySync;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // === Getters and Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ChannelType getType() { return type; }
    public void setType(ChannelType type) { this.type = type; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }

    public String getSyncSettings() { return syncSettings; }
    public void setSyncSettings(String syncSettings) { this.syncSettings = syncSettings; }

    public LocalDateTime getLastProductSync() { return lastProductSync; }
    public void setLastProductSync(LocalDateTime lastProductSync) { this.lastProductSync = lastProductSync; }

    public LocalDateTime getLastOrderSync() { return lastOrderSync; }
    public void setLastOrderSync(LocalDateTime lastOrderSync) { this.lastOrderSync = lastOrderSync; }

    public LocalDateTime getLastInventorySync() { return lastInventorySync; }
    public void setLastInventorySync(LocalDateTime lastInventorySync) { this.lastInventorySync = lastInventorySync; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
