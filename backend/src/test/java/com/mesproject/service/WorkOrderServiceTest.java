package com.mesproject.service;

import com.mesproject.entity.Product;
import com.mesproject.entity.WorkOrder;
import com.mesproject.repo.ProcessRepository;
import com.mesproject.repo.ProductRepository;
import com.mesproject.repo.WorkOrderRepository;
import com.mesproject.workorder.CreateWorkOrderRequest;
import com.mesproject.workorder.WorkOrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProcessRepository processRepository;

    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() {
        workOrderService = new WorkOrderService(workOrderRepository, productRepository, processRepository);
    }

    @Test
    void create_setsInitialStatusAndProducedQty() {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 1L);

        com.mesproject.entity.Process process = new com.mesproject.entity.Process();
        ReflectionTestUtils.setField(process, "id", 2L);

        CreateWorkOrderRequest req = new CreateWorkOrderRequest(
                "WO-20260327-001",
                1L,
                2L,
                120,
                LocalDate.of(2026, 3, 27)
        );

        when(workOrderRepository.existsByWorkOrderNo(req.workOrderNo())).thenReturn(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(processRepository.findById(2L)).thenReturn(Optional.of(process));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder wo = invocation.getArgument(0);
            ReflectionTestUtils.setField(wo, "id", 100L);
            return wo;
        });

        WorkOrderResponse response = workOrderService.create(req);

        assertEquals(100L, response.id());
        assertEquals("WO-20260327-001", response.workOrderNo());
        assertEquals(1L, response.productId());
        assertEquals(2L, response.processId());
        assertEquals(120, response.plannedQty());
        assertEquals(0, response.producedQty());
        assertEquals("PLANNED", response.status());
        assertEquals(LocalDate.of(2026, 3, 27), response.plannedDate());
    }

    @Test
    void create_throwsConflictWhenDuplicateWorkOrderNo() {
        CreateWorkOrderRequest req = new CreateWorkOrderRequest(
                "WO-20260327-001",
                1L,
                2L,
                120,
                LocalDate.of(2026, 3, 27)
        );

        when(workOrderRepository.existsByWorkOrderNo(req.workOrderNo())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> workOrderService.create(req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
