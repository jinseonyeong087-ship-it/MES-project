package com.mesproject.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_result")
public class ProductionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "good_qty", nullable = false)
    private Integer goodQty;

    @Column(name = "defect_qty", nullable = false)
    private Integer defectQty;

    @Column(name = "result_at", nullable = false)
    private LocalDateTime resultAt;

    @Column(name = "[operator]")
    private String operator;

    @Column(name = "memo")
    private String memo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public Integer getGoodQty() { return goodQty; }
    public Integer getDefectQty() { return defectQty; }
    public LocalDateTime getResultAt() { return resultAt; }
    public String getOperator() { return operator; }
    public String getMemo() { return memo; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public void setGoodQty(Integer goodQty) { this.goodQty = goodQty; }
    public void setDefectQty(Integer defectQty) { this.defectQty = defectQty; }
    public void setResultAt(LocalDateTime resultAt) { this.resultAt = resultAt; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setMemo(String memo) { this.memo = memo; }
}
