package com.mesproject.repo;

import com.mesproject.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query("select i from Inventory i join fetch i.product where i.product.id = :productId")
    Optional<Inventory> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i join fetch i.product where i.product.id = :productId")
    Optional<Inventory> findByProductIdForUpdate(Long productId);
}
