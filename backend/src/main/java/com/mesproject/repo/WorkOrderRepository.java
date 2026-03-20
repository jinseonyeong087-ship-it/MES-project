package com.mesproject.repo;

import com.mesproject.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    boolean existsByWorkOrderNo(String workOrderNo);
    Optional<WorkOrder> findByWorkOrderNo(String workOrderNo);
    java.util.List<WorkOrder> findAllByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkOrder w where w.id = :id")
    Optional<WorkOrder> findByIdForUpdate(Long id);
}
