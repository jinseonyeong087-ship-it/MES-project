package com.mesproject.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_log")
public class InventoryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "change_type", nullable = false)
    private String changeType;

    @Column(name = "ref_type", nullable = false)
    private String refType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(name = "before_qty", nullable = false)
    private Integer beforeQty;

    @Column(name = "change_qty", nullable = false)
    private Integer changeQty;

    @Column(name = "after_qty", nullable = false)
    private Integer afterQty;

    @Column(name = "changed_at", insertable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by")
    private String changedBy;

    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public void setRefType(String refType) { this.refType = refType; }
    public void setRefId(Long refId) { this.refId = refId; }
    public void setBeforeQty(Integer beforeQty) { this.beforeQty = beforeQty; }
    public void setChangeQty(Integer changeQty) { this.changeQty = changeQty; }
    public void setAfterQty(Integer afterQty) { this.afterQty = afterQty; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
}
