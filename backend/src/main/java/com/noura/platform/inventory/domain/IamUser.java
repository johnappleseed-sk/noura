package com.noura.platform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "iam_users")
public class IamUser extends SoftDeleteEntity {

    @Column(name = "username", nullable = false, length = 120, unique = true)
    private String username;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(name = "status", nullable = false, length = 40)
    private String status = "ACTIVE";

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
