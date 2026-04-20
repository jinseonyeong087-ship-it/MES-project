package com.mesproject.service;

import com.mesproject.entity.Product;
import com.mesproject.entity.WorkOrder;
import com.mesproject.repo.ProcessRepository;
import com.mesproject.repo.ProductRepository;
import com.mesproject.repo.WorkOrderRepository;
import com.mesproject.workorder.CreateWorkOrderRequest;
import com.mesproject.workorder.WorkOrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final ProductRepository productRepository;
    private final ProcessRepository processRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            ProductRepository productRepository,
                            ProcessRepository processRepository) {
        this.workOrderRepository = workOrderRepository;
        this.productRepository = productRepository;
        this.processRepository = processRepository;
    }

    // 최근 등록 작업지시를 먼저 보여주기 위해 ID 역순 조회
    public List<WorkOrderResponse> getAll() {
        return workOrderRepository.findAllByOrderByIdDesc().stream().map(w -> new WorkOrderResponse(
                w.getId(),
                w.getWorkOrderNo(),
                w.getProduct().getId(),
                w.getProcess().getId(),
                w.getPlannedQty(),
                w.getProducedQty(),
                w.getStatus(),
                w.getPlannedDate(),
                w.getCreatedAt()
        )).toList();
    }

    // 작업지시 생성: 중복 번호/기준정보 존재 여부를 먼저 검증
    public WorkOrderResponse create(CreateWorkOrderRequest request) {
        if (workOrderRepository.existsByWorkOrderNo(request.workOrderNo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate workOrderNo");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        com.mesproject.entity.Process process = processRepository.findById(request.processId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Process not found"));

        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderNo(request.workOrderNo());
        wo.setProduct(product);
        wo.setProcess(process);
        wo.setPlannedQty(request.plannedQty());
        wo.setProducedQty(0);
        // 최초 상태는 반드시 PLANNED
        wo.setStatus("PLANNED");
        wo.setPlannedDate(request.plannedDate());

        WorkOrder saved = workOrderRepository.save(wo);

        return new WorkOrderResponse(
                saved.getId(),
                saved.getWorkOrderNo(),
                saved.getProduct().getId(),
                saved.getProcess().getId(),
                saved.getPlannedQty(),
                saved.getProducedQty(),
                saved.getStatus(),
                saved.getPlannedDate(),
                saved.getCreatedAt()
        );
    }
}
