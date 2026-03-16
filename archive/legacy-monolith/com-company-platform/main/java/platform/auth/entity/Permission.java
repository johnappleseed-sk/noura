package com.company.platform.auth.entity;

import com.noura.platform.domain.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "admin_permissions")
public class Permission extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scope", nullable = false, length = 80)
    private String scope;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "label", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 600)
    private String description;

    @OneToMany(mappedBy = "permission", fetch = FetchType.LAZY)
    private Set<RolePermission> rolePermissions = new LinkedHashSet<>();

    @Transient
    public String getCode() {
        return (scope + "_" + action)
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
