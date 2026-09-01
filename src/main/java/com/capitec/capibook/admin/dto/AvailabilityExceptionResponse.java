package com.capitec.capibook.admin.dto;

import com.capitec.capibook.branch.BranchAvailabilityException;
import com.capitec.capibook.branch.ExceptionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AvailabilityExceptionResponse(
        UUID id,
        UUID branchId,
        LocalDate exceptionDate,
        ExceptionType type,
        String reason,
        LocalDateTime createdAt
) {
    public static AvailabilityExceptionResponse from(BranchAvailabilityException e) {
        return new AvailabilityExceptionResponse(
                e.getId(),
                e.getBranch().getId(),
                e.getExceptionDate(),
                e.getType(),
                e.getReason(),
                e.getCreatedAt()
        );
    }
}
