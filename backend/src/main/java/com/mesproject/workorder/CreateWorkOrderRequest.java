package com.mesproject.workorder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateWorkOrderRequest(
        @NotBlank String workOrderNo,
        @NotNull Long productId,
        @NotNull Long processId,
        @NotNull @Min(1) Integer plannedQty,
        @NotNull LocalDate plannedDate
) {}
