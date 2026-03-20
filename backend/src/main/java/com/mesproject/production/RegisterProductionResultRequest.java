package com.mesproject.production;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RegisterProductionResultRequest(
        @NotNull Long workOrderId,
        @NotNull @Min(0) Integer goodQty,
        @NotNull @Min(0) Integer defectQty,
        @NotNull LocalDateTime resultAt,
        @NotBlank String operator,
        String memo
) {}
