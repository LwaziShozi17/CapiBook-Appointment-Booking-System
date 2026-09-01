package com.capitec.capibook.servicecatalog.dto;

import com.capitec.capibook.servicecatalog.BankingService;

import java.time.LocalDateTime;
import java.util.UUID;

public record BankingServiceResponse(
        UUID id,
        String name,
        String description,
        int durationMinutes,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BankingServiceResponse from(BankingService service) {
        return new BankingServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.isActive(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
