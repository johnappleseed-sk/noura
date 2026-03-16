package com.company.platform.auth.repository;

import com.company.platform.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCodeIgnoreCase(String code);

    List<Role> findByCodeIn(Collection<String> codes);
}
