package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.PriceListType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "price_lists")
public class PriceList extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceListType type;

    @Column(name = "customer_group_id")
    private UUID customerGroupId;

    @Column(name = "channel_id")
    private UUID channelId;
}
