package com.mesproject.workorder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkOrderResponse(
        Long id,
        String workOrderNo,
        Long productId,
        Long processId,
        Integer plannedQty,
        Integer producedQty,
        String status,
        LocalDate plannedDate,
        LocalDateTime createdAt
) {}
