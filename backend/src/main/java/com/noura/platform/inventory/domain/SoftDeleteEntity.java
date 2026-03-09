package com.noura.platform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
public abstract class SoftDeleteEntity extends AuditedEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;
}
