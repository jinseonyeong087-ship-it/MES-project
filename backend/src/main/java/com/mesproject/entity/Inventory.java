package com.mesproject.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "qty_on_hand", nullable = false)
    private Integer qtyOnHand;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Integer getQtyOnHand() { return qtyOnHand; }
    public Integer getSafetyStock() { return safetyStock; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setProduct(Product product) { this.product = product; }
    public void setQtyOnHand(Integer qtyOnHand) { this.qtyOnHand = qtyOnHand; }
    public void setSafetyStock(Integer safetyStock) { this.safetyStock = safetyStock; }
}
