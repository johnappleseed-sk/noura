package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.CategoryChangeAction;
import com.noura.platform.domain.enums.CategoryChangeRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "category_change_requests")
public class CategoryChangeRequest extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoryChangeAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoryChangeRequestStatus status = CategoryChangeRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id")
    private UserAccount requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private UserAccount reviewedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "json", nullable = false)
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Column(length = 1000)
    private String reason;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
