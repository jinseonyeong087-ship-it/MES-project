package com.mesproject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit", nullable = false)
    private String unit;

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public String getUnit() { return unit; }
}
