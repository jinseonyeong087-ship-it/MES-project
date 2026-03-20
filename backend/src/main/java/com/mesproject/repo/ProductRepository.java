package com.mesproject.repo;

import com.mesproject.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.List<Product> findAllByOrderByIdAsc();
}
