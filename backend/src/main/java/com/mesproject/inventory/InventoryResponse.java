package com.mesproject.inventory;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long productId,
        String productCode,
        String productName,
        Integer qtyOnHand,
        Integer safetyStock,
        LocalDateTime updatedAt
) {}
