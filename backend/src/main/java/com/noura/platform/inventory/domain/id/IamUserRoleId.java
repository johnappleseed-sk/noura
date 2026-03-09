package com.noura.platform.inventory.domain.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class IamUserRoleId implements Serializable {

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "role_id", nullable = false, length = 36)
    private String roleId;
}
