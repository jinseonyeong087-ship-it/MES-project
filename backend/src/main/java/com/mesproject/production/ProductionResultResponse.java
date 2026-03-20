package com.mesproject.production;

import java.time.LocalDateTime;

public record ProductionResultResponse(
        Long productionResultId,
        Long workOrderId,
        String workOrderStatus,
        Integer accumulatedProducedQty,
        InventorySnapshot inventory,
        Boolean logged,
        LocalDateTime createdAt
) {
    public record InventorySnapshot(
            Long productId,
            Integer beforeQty,
            Integer changeQty,
            Integer afterQty
    ) {}
}
