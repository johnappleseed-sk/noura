package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.IamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IamRoleRepository extends JpaRepository<IamRole, String> {

    Optional<IamRole> findByCodeIgnoreCase(String code);

    List<IamRole> findAllByCodeIn(Collection<String> codes);
}
