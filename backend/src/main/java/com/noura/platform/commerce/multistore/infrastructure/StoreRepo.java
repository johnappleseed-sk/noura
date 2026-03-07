package com.noura.platform.commerce.multistore.infrastructure;

import com.noura.platform.commerce.multistore.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepo extends JpaRepository<Store, Long> {

    Optional<Store> findByCode(String code);

    List<Store> findByActiveTrue();

    List<Store> findByActiveTrueOrderByName();

    boolean existsByCode(String code);
}
