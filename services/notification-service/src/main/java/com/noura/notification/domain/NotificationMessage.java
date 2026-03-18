package com.noura.notification.domain;

import com.noura.notification.domain.enums.NotificationChannel;
import com.noura.notification.domain.enums.NotificationStatus;
import com.noura.notification.domain.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notification_messages")
public class NotificationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "related_entity_type", length = 80)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        subject = trim(subject);
        body = trim(body);
        relatedEntityType = trim(relatedEntityType);
        failureReason = trim(failureReason);
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        subject = trim(subject);
        body = trim(body);
        relatedEntityType = trim(relatedEntityType);
        failureReason = trim(failureReason);
        updatedAt = Instant.now();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
