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

    @Transactional
    public ProductionResultResponse register(RegisterProductionResultRequest req) {
        if (req.goodQty() < 0 || req.defectQty() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Qty must be >= 0");
        }

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
        if (newProduced >= wo.getPlannedQty()) wo.setStatus("COMPLETED");
        else wo.setStatus("IN_PROGRESS");

        Inventory inv = inventoryRepository.findByProductIdForUpdate(wo.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found for product"));

        int before = inv.getQtyOnHand();
        int change = req.goodQty();
        inv.setQtyOnHand(before + change);
        int after = inv.getQtyOnHand();

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
