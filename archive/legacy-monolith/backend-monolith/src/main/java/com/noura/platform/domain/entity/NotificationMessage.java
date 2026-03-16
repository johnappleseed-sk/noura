package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.NotificationChannel;
import com.noura.platform.domain.enums.NotificationStatus;
import com.noura.platform.domain.enums.NotificationType;
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
public class NotificationMessage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private UserAccount recipientUser;

    @Column(name = "recipient_user_id", insertable = false, updatable = false)
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

    @PrePersist
    @PreUpdate
    void normalize() {
        subject = trim(subject);
        body = trim(body);
        relatedEntityType = trim(relatedEntityType);
        failureReason = trim(failureReason);
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
        if (status == NotificationStatus.SENT) {
            failedAt = null;
            if (sentAt == null) {
                sentAt = Instant.now();
            }
        }
        if (status == NotificationStatus.FAILED) {
            sentAt = null;
            if (failedAt == null) {
                failedAt = Instant.now();
            }
        }
        if (status == NotificationStatus.PENDING) {
            sentAt = null;
            failedAt = null;
            failureReason = null;
        }
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
