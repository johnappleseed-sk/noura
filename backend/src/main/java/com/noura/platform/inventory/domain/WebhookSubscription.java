package com.noura.platform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription extends AuditedEntity {

    @Column(name = "event_code", nullable = false, length = 120)
    private String eventCode;

    @Column(name = "endpoint_url", nullable = false, length = 1000)
    private String endpointUrl;

    @Column(name = "secret_token", length = 255)
    private String secretToken;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs = 5000;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 3;
}
