package com.mesproject.repo;

import com.mesproject.entity.ProductionResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionResultRepository extends JpaRepository<ProductionResult, Long> {
    java.util.List<ProductionResult> findTop100ByOrderByResultAtDesc();
}
