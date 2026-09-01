package com.capitec.capibook.admin.dto;

import com.capitec.capibook.branch.ExceptionType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAvailabilityExceptionRequest(
        @NotNull LocalDate exceptionDate,
        @NotNull ExceptionType type,
        String reason
) {}
