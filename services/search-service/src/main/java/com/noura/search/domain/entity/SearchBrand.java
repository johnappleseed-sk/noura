package com.noura.search.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Getter
@Setter
@Immutable
@Entity
@Table(name = "brands")
public class SearchBrand {

    @Id
    private UUID id;

    private String name;

    private boolean active;
}
