package com.noura.platform.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "channel_category_mappings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_channel_category_mapping_category_channel_region",
                        columnNames = {"category_id", "channel", "region_code"}
                )
        }
)
public class ChannelCategoryMapping extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 60)
    private String channel;

    @Column(name = "region_code", nullable = false, length = 16)
    private String regionCode = "GLOBAL";

    @Column(name = "external_category_id", nullable = false, length = 255)
    private String externalCategoryId;

    @Column(name = "external_category_name", length = 255)
    private String externalCategoryName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
