package com.noura.search.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Read-only source projection for canonical category metadata used during search index rebuilds.
 */
@Getter
@Setter
@Immutable
@Entity
@Table(name = "categories")
public class SearchCategory {

    @Id
    private UUID id;

    private String name;
}
