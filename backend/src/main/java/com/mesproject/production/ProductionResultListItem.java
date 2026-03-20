package com.mesproject.production;

import java.time.LocalDateTime;

public record ProductionResultListItem(
        Long productionResultId,
        Long workOrderId,
        Integer goodQty,
        Integer defectQty,
        String operator,
        LocalDateTime resultAt
) {}
