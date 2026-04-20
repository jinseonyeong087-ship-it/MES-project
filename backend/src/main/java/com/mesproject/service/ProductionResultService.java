package com.mesproject.service;

import com.mesproject.entity.Inventory;
import com.mesproject.entity.InventoryLog;
import com.mesproject.entity.ProductionResult;
import com.mesproject.entity.WorkOrder;
import com.mesproject.production.ProductionResultListItem;
import com.mesproject.production.ProductionResultResponse;
import com.mesproject.production.RegisterProductionResultRequest;
import com.mesproject.repo.InventoryLogRepository;
import com.mesproject.repo.InventoryRepository;
import com.mesproject.repo.ProductionResultRepository;
import com.mesproject.repo.WorkOrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductionResultService {
    private final WorkOrderRepository workOrderRepository;
    private final ProductionResultRepository productionResultRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public ProductionResultService(WorkOrderRepository workOrderRepository,
                                   ProductionResultRepository productionResultRepository,
                                   InventoryRepository inventoryRepository,
                                   InventoryLogRepository inventoryLogRepository) {
        this.workOrderRepository = workOrderRepository;
        this.productionResultRepository = productionResultRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    // 최근 실적 100건을 조회해 대시보드 초기 로딩에 사용
    public List<ProductionResultListItem> getRecent() {
        return productionResultRepository.findTop100ByOrderByResultAtDesc().stream().map(p ->
                new ProductionResultListItem(
                        p.getId(),
                        p.getWorkOrder().getId(),
                        p.getGoodQty(),
                        p.getDefectQty(),
                        p.getOperator(),
                        p.getResultAt()
                )
        ).toList();
    }

    // 실적 등록, 작업지시 상태 반영, 재고/로그 반영을 하나의 트랜잭션으로 처리
    @Transactional
    public ProductionResultResponse register(RegisterProductionResultRequest req) {
        if (req.goodQty() < 0 || req.defectQty() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Qty must be >= 0");
        }

        // 동시성 충돌 방지를 위해 작업지시를 잠금 조회
        WorkOrder wo = workOrderRepository.findByIdForUpdate(req.workOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkOrder not found"));

        if ("COMPLETED".equals(wo.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "COMPLETED work order cannot accept more results");
        }

        ProductionResult pr = new ProductionResult();
        pr.setWorkOrder(wo);
        pr.setGoodQty(req.goodQty());
        pr.setDefectQty(req.defectQty());
        pr.setResultAt(req.resultAt());
        pr.setOperator(req.operator());
        pr.setMemo(req.memo());
        ProductionResult savedPr = productionResultRepository.save(pr);

        int produced = req.goodQty() + req.defectQty();
        int newProduced = wo.getProducedQty() + produced;
        wo.setProducedQty(newProduced);
        wo.setStatus(newProduced >= wo.getPlannedQty() ? "COMPLETED" : "IN_PROGRESS");

        // 동시성 충돌 방지를 위해 재고도 잠금 조회
        Inventory inv = inventoryRepository.findByProductIdForUpdate(wo.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found for product"));

        int before = inv.getQtyOnHand();
        int change = req.goodQty(); // 양품만 재고 증가 반영
        inv.setQtyOnHand(before + change);
        int after = inv.getQtyOnHand();

        // 감사 추적용 재고 이력 기록
        InventoryLog log = new InventoryLog();
        log.setInventory(inv);
        log.setChangeType("PRODUCTION_IN");
        log.setRefType("PRODUCTION_RESULT");
        log.setRefId(savedPr.getId());
        log.setBeforeQty(before);
        log.setChangeQty(change);
        log.setAfterQty(after);
        log.setChangedBy(req.operator());
        inventoryLogRepository.save(log);

        return new ProductionResultResponse(
                savedPr.getId(),
                wo.getId(),
                wo.getStatus(),
                wo.getProducedQty(),
                new ProductionResultResponse.InventorySnapshot(
                        wo.getProduct().getId(),
                        before,
                        change,
                        after
                ),
                true,
                savedPr.getCreatedAt()
        );
    }
}
