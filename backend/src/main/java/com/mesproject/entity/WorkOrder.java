package com.mesproject.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_order")
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_no", nullable = false, unique = true)
    private String workOrderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private Process process;

    @Column(name = "planned_qty", nullable = false)
    private Integer plannedQty;

    @Column(name = "produced_qty", nullable = false)
    private Integer producedQty;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getWorkOrderNo() { return workOrderNo; }
    public Product getProduct() { return product; }
    public Process getProcess() { return process; }
    public Integer getPlannedQty() { return plannedQty; }
    public Integer getProducedQty() { return producedQty; }
    public String getStatus() { return status; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setWorkOrderNo(String workOrderNo) { this.workOrderNo = workOrderNo; }
    public void setProduct(Product product) { this.product = product; }
    public void setProcess(Process process) { this.process = process; }
    public void setPlannedQty(Integer plannedQty) { this.plannedQty = plannedQty; }
    public void setProducedQty(Integer producedQty) { this.producedQty = producedQty; }
    public void setStatus(String status) { this.status = status; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
}
