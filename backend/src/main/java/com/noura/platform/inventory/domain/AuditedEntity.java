package com.noura.platform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
public abstract class AuditedEntity extends CreatedEntity {

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
