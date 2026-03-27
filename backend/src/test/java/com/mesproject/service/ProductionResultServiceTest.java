package com.mesproject.service;

import com.mesproject.entity.Inventory;
import com.mesproject.entity.InventoryLog;
import com.mesproject.entity.Product;
import com.mesproject.entity.ProductionResult;
import com.mesproject.entity.WorkOrder;
import com.mesproject.production.ProductionResultResponse;
import com.mesproject.production.RegisterProductionResultRequest;
import com.mesproject.repo.InventoryLogRepository;
import com.mesproject.repo.InventoryRepository;
import com.mesproject.repo.ProductionResultRepository;
import com.mesproject.repo.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionResultServiceTest {

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private ProductionResultRepository productionResultRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryLogRepository inventoryLogRepository;

    private ProductionResultService productionResultService;

    @BeforeEach
    void setUp() {
        productionResultService = new ProductionResultService(
                workOrderRepository,
                productionResultRepository,
                inventoryRepository,
                inventoryLogRepository
        );
    }

    @Test
    void register_updatesWorkOrderInventoryAndCreatesLog() {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 1L);

        WorkOrder wo = new WorkOrder();
        ReflectionTestUtils.setField(wo, "id", 10L);
        wo.setProduct(product);
        wo.setPlannedQty(100);
        wo.setProducedQty(90);
        wo.setStatus("IN_PROGRESS");
        wo.setPlannedDate(LocalDate.of(2026, 3, 27));

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQtyOnHand(50);
        inventory.setSafetyStock(10);

        RegisterProductionResultRequest req = new RegisterProductionResultRequest(
                10L,
                8,
                3,
                LocalDateTime.of(2026, 3, 27, 20, 0),
                "operator-a",
                "1차 생산"
        );

        when(workOrderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(wo));
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(productionResultRepository.save(any(ProductionResult.class))).thenAnswer(invocation -> {
            ProductionResult pr = invocation.getArgument(0);
            ReflectionTestUtils.setField(pr, "id", 777L);
            return pr;
        });

        ProductionResultResponse response = productionResultService.register(req);

        assertEquals("COMPLETED", wo.getStatus());
        assertEquals(101, wo.getProducedQty());
        assertEquals(58, inventory.getQtyOnHand());

        assertEquals(777L, response.productionResultId());
        assertEquals(10L, response.workOrderId());
        assertEquals("COMPLETED", response.workOrderStatus());
        assertEquals(101, response.accumulatedProducedQty());
        assertEquals(50, response.inventory().beforeQty());
        assertEquals(8, response.inventory().changeQty());
        assertEquals(58, response.inventory().afterQty());
        assertTrue(response.logged());

        ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(logCaptor.capture());
        InventoryLog savedLog = logCaptor.getValue();
        assertNotNull(savedLog);
    }

    @Test
    void register_throwsConflictWhenWorkOrderAlreadyCompleted() {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 1L);

        WorkOrder wo = new WorkOrder();
        ReflectionTestUtils.setField(wo, "id", 10L);
        wo.setProduct(product);
        wo.setPlannedQty(100);
        wo.setProducedQty(100);
        wo.setStatus("COMPLETED");

        RegisterProductionResultRequest req = new RegisterProductionResultRequest(
                10L,
                1,
                0,
                LocalDateTime.of(2026, 3, 27, 20, 30),
                "operator-a",
                null
        );

        when(workOrderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(wo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productionResultService.register(req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(productionResultRepository, never()).save(any());
        verify(inventoryRepository, never()).findByProductIdForUpdate(any());
        verify(inventoryLogRepository, never()).save(any());
    }
}
