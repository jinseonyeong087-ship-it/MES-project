package com.mesproject.workorder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateWorkOrderRequest(
        @NotBlank
        @Size(max = 40)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "영문/숫자/_/- 만 허용")
        String workOrderNo,
        @NotNull Long productId,
        @NotNull Long processId,
        @NotNull @Min(1) Integer plannedQty,
        @NotNull LocalDate plannedDate
) {}
